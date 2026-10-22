#!/bin/sh
# One release operation at a time. Builds happen on the development machine.
set -eu
exec 9>/var/lock/myblog-release.lock
flock -n 9 || { echo 'Another My-blog release is in progress' >&2; exit 1; }
available_kib=$(awk '/^MemAvailable:/ {print $2}' /proc/meminfo)
free_kib=$(df -Pk / | awk 'NR==2 {print $4}')
[ "$available_kib" -ge 524288 ] || { echo 'Release paused: less than 512 MiB available' >&2; exit 1; }
[ "$free_kib" -ge 2097152 ] || { echo 'Release paused: less than 2 GiB disk available' >&2; exit 1; }
printf 'release_headroom mem_available_kib=%s disk_available_kib=%s\n' "$available_kib" "$free_kib"
if [ "$#" -gt 0 ]; then
  exec "$@"
fi
