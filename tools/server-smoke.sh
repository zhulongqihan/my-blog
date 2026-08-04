#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-https://cyruszhang.online}"

check_status() {
  local path="$1"
  local expected="$2"
  local result
  result="$(curl -sS --max-time 20 -o /dev/null -w '%{http_code} %{time_total}' "${BASE_URL}${path}")"
  local status="${result%% *}"
  local duration="${result#* }"
  printf '%-32s HTTP %s  %ss\n' "$path" "$status" "$duration"
  if [[ "$status" != "$expected" ]]; then
    echo "Unexpected status for ${path}: expected ${expected}, got ${status}" >&2
    exit 1
  fi
}

check_status "/" 200
check_status "/robots.txt" 200
check_status "/sitemap.xml" 200
check_status "/api/articles?page=0&size=1" 200
check_status "/api/articles/archive" 200
check_status "/api/articles/54" 200
check_status "/.env" 404
check_status "/actuator" 404

echo "Server smoke checks passed: ${BASE_URL}"
