#!/usr/bin/env python3
"""Local, transition-based capacity alarm. Never reads credentials or sends messages."""
import argparse
import json
import os
from pathlib import Path
import time


def read_numbers(path):
    result = {}
    for line in Path(path).read_text().splitlines():
        parts = line.split()
        if len(parts) > 1 and parts[1].isdigit():
            result[parts[0].rstrip(":")] = int(parts[1])
    return result


def sample(path="/"):
    fs = os.statvfs(path)
    mem = read_numbers("/proc/meminfo")
    before = read_numbers("/proc/vmstat")
    started = time.monotonic()
    time.sleep(1)
    after = read_numbers("/proc/vmstat")
    elapsed = time.monotonic() - started
    page_kib = os.sysconf("SC_PAGE_SIZE") / 1024
    return {
        "disk_percent": round(100 * (fs.f_blocks - fs.f_bavail) / fs.f_blocks, 2),
        "inode_percent": round(100 * (fs.f_files - fs.f_favail) / fs.f_files, 2) if fs.f_files else 0,
        "mem_available_mib": round(mem["MemAvailable"] / 1024, 2),
        "swap_in_kib_s": round((after["pswpin"] - before["pswpin"]) * page_kib / elapsed, 2),
        "swap_out_kib_s": round((after["pswpout"] - before["pswpout"]) * page_kib / elapsed, 2),
    }


def assess(metrics, disk=85, inode=85, memory=256, swap=4096):
    alarms = []
    if metrics["disk_percent"] >= disk:
        alarms.append("disk_capacity")
    if metrics["inode_percent"] >= inode:
        alarms.append("inode_capacity")
    if metrics["mem_available_mib"] < memory:
        alarms.append("memory_pressure")
    if metrics["swap_in_kib_s"] + metrics["swap_out_kib_s"] >= swap:
        alarms.append("active_swapping")
    return alarms


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--state-dir", default="/var/lib/myblog-health")
    parser.add_argument("--path", default="/")
    args = parser.parse_args()
    metrics = sample(args.path)
    alarms = assess(metrics)
    state_dir = Path(args.state_dir)
    state_dir.mkdir(parents=True, exist_ok=True)
    target = state_dir / "state.json"
    previous = json.loads(target.read_text()) if target.exists() else {}
    state = {"time": time.time(), "alarms": alarms, "metrics": metrics}
    temporary = state_dir / "state.tmp"
    temporary.write_text(json.dumps(state, sort_keys=True))
    temporary.replace(target)
    if alarms != previous.get("alarms", []):
        print(json.dumps({"event": "capacity_alert" if alarms else "capacity_recovered", **state}))
    return 1 if alarms else 0


if __name__ == "__main__":
    raise SystemExit(main())
