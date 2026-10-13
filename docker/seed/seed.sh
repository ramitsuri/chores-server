#!/usr/bin/env bash
# Waits for the server to create its tables, then inserts dummy data.
set -euo pipefail

echo "seed: waiting for server to create tables..."
for _ in $(seq 1 150); do
  ready=$(psql -tAc "select to_regclass('public.taskassignments') is not null
                       and to_regclass('public.pushmessagetokens') is not null" 2>/dev/null || true)
  if [ "$ready" = "t" ]; then
    psql -v ON_ERROR_STOP=1 -q -f /seed/seed.sql
    echo "seed: dummy data inserted"
    psql -c "select (select count(*) from members) as members,
                    (select count(*) from houses) as houses,
                    (select count(*) from tasks) as tasks,
                    (select count(*) from taskassignments) as assignments"
    exit 0
  fi
  sleep 2
done

echo "seed: timed out waiting for tables" >&2
exit 1
