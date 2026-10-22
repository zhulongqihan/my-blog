#!/usr/bin/env python3
"""Destructive checks only inside labelled, disposable test containers/volume."""
import argparse
import glob
import json
from pathlib import Path
import socket
import subprocess
import time
import uuid


def docker(*args):
    return subprocess.check_output(["docker", *args], universal_newlines=True, timeout=45).strip()


class Redis:
    def __init__(self, port):
        self.socket = socket.create_connection(("127.0.0.1", port), 5)
        self.socket.settimeout(5)
        self.file = self.socket.makefile("rb")

    def command(self, *args):
        parts = [x if isinstance(x, bytes) else str(x).encode() for x in args]
        self.socket.sendall(b"*%d\r\n" % len(parts) + b"".join(
            b"$%d\r\n" % len(x) + x + b"\r\n" for x in parts))
        return self.read()

    def read(self):
        line = self.file.readline()
        if not line:
            raise EOFError("Redis closed connection")
        kind, value = line[:1], line[1:-2]
        if kind == b"-":
            raise RuntimeError(value.decode())
        if kind == b"+":
            return value.decode()
        if kind == b":":
            return int(value)
        if kind == b"$":
            size = int(value)
            if size < 0:
                return None
            result = self.file.read(size)
            self.file.read(2)
            return result.decode()
        if kind == b"*":
            return [self.read() for _ in range(int(value))]
        raise ValueError("Invalid Redis reply")

    def close(self):
        self.file.close()
        self.socket.close()


def connect(name):
    port = int(json.loads(docker("inspect", name))[0]["NetworkSettings"]["Ports"]["6379/tcp"][0]["HostPort"])
    deadline = time.monotonic() + 15
    while True:
        try:
            client = Redis(port)
            if client.command("PING") == "PONG":
                return client
        except (OSError, EOFError):
            if time.monotonic() >= deadline:
                raise
            time.sleep(.1)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--image", required=True, help="Existing Redis image ID; no pull or upgrade")
    args = parser.parse_args()
    memory = dict(line.split(":", 1) for line in Path("/proc/meminfo").read_text().splitlines())
    if int(memory["MemAvailable"].split()[0]) < 512 * 1024:
        raise RuntimeError("Insufficient host headroom for isolated tests (need 512 MiB available)")
    token = "codex-phase1-infra-" + uuid.uuid4().hex[:8]
    label = "codex.phase-one-test=" + token
    volume = token + "-data"
    names = []
    clients = []
    results = {}
    docker("volume", "create", "--label", label, volume)
    try:
        for suffix, persistence in (("state", True), ("cache", False)):
            name = token + "-" + suffix
            names.append(name)
            cmd = ["run", "-d", "--name", name, "--label", label, "--memory", "32m",
                   "--cpus", "0.25", "--pids-limit", "32", "-p", "127.0.0.1::6379",
                   "--log-opt", "max-size=16k", "--log-opt", "max-file=3"]
            if persistence:
                cmd += ["-v", volume + ":/data"]
            cmd += [args.image, "redis-server", "--save", "", "--maxmemory", "4mb",
                    "--maxmemory-policy", "noeviction" if persistence else "allkeys-lru",
                    "--appendonly", "yes" if persistence else "no", "--appendfsync", "always"]
            docker(*cmd)
            clients.append(connect(name))
        state, cache = clients
        state.command("SET", "jwt:blacklist:test-token", "revoked", "EX", 600)
        state.command("SET", "article:view:count:1", "17")
        state.command("HSET", "article:view:pending:1", "batchId", "recover-me", "delta", "9")
        state.command("SADD", "article:liked:1", "reader-1")
        for index in range(400):
            cache.command("SET", "article:detail:" + str(index), b"x" * 32768)
        info = dict(line.split(":", 1) for line in cache.command("INFO", "stats").splitlines()
                    if ":" in line and not line.startswith("#"))
        assert int(info["evicted_keys"]) > 0
        assert state.command("GET", "jwt:blacklist:test-token") == "revoked"
        results["cache_evicted_keys"] = int(info["evicted_keys"])
        results["state_unchanged_during_cache_eviction"] = True
        state.close()
        docker("kill", "--signal", "KILL", names[0])
        docker("start", names[0])
        state = connect(names[0])
        clients[0] = state
        assert state.command("GET", "jwt:blacklist:test-token") == "revoked"
        assert state.command("GET", "article:view:count:1") == "17"
        assert state.command("HGETALL", "article:view:pending:1") == ["batchId", "recover-me", "delta", "9"]
        assert state.command("SISMEMBER", "article:liked:1", "reader-1") == 1
        results["sigkill_aof_recovery"] = ["revocation", "counter", "pending_batch", "like_dedup"]
        rejected = False
        for index in range(400):
            try:
                state.command("SET", "pressure:" + str(index), b"x" * 32768)
            except RuntimeError as error:
                assert "OOM" in str(error)
                rejected = True
                break
        assert rejected
        assert state.command("GET", "jwt:blacklist:test-token") == "revoked"
        results["state_capacity_rejects_without_eviction"] = True

        log_name = token + "-logs"
        names.append(log_name)
        producer = "i=0; while [ $i -lt 600 ]; do printf '%s\\n' '" + "L" * 1024 + "'; i=$((i+1)); done"
        docker("run", "-d", "--name", log_name, "--label", label, "--memory", "16m",
               "--pids-limit", "16", "--log-driver", "json-file", "--log-opt", "max-size=16k",
               "--log-opt", "max-file=3", args.image, "sh", "-c", producer)
        assert docker("wait", log_name) == "0"
        log_path = json.loads(docker("inspect", log_name))[0]["LogPath"]
        logs = list(glob.glob(log_path + "*"))
        sizes = [Path(x).stat().st_size for x in logs]
        assert 2 <= len(logs) <= 3 and all(size <= 18000 for size in sizes), sizes
        results["log_rotation"] = {"files": len(logs), "bytes": sizes, "produced_bytes": 600 * 1025}
        print(json.dumps({"test": token, "passed": True, "results": results}, sort_keys=True))
    finally:
        for client in clients:
            client.close()
        for name in names:
            detail = json.loads(docker("inspect", name))[0]
            if detail["Config"]["Labels"].get("codex.phase-one-test") == token:
                docker("rm", "-f", name)
        if json.loads(docker("volume", "inspect", volume))[0]["Labels"].get("codex.phase-one-test") == token:
            docker("volume", "rm", volume)


if __name__ == "__main__":
    main()
