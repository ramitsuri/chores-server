# Local Docker environment

Postgres 17 + the Chores server + dummy data. **Nothing is persisted**: the database lives in memory and is wiped whenever the containers stop.

```bash
# from the repo root
docker compose -f -d docker/compose.yaml up --build   # build, start, seed (Ctrl+C to stop)
docker compose -f docker/compose.yaml down         # remove containers
```

| What     | Where                                                              |
|----------|--------------------------------------------------------------------|
| Server   | `http://localhost:8081`                                            |
| Postgres | `localhost:5433`, db `chores`, user/password `postgres`/`postgres` |

Push messages are disabled (`PUSH_MESSAGES_ENABLED=false`) because there is no Firebase service account locally. Pushes are logged instead of sent.

## Test data

Every member's login key is `test-key`.

| Member | ID                                     | Access                 | Houses              |
|--------|----------------------------------------|------------------------|---------------------|
| Admin  | `aaaaaaaa-0000-0000-0000-000000000001` | READ_ALL_WRITE_ALL     | Home, Cabin, Office |
| Alice  | `aaaaaaaa-0000-0000-0000-000000000002` | READ_HOUSE_WRITE_HOUSE | Home, Cabin         |
| Bob    | `aaaaaaaa-0000-0000-0000-000000000003` | READ_HOUSE_WRITE_OWN   | Home, Cabin         |
| Carol  | `aaaaaaaa-0000-0000-0000-000000000004` | READ_HOUSE_WRITE_OWN   | Office              |

| House  | ID                                     | Status |
|--------|----------------------------------------|--------|
| Home   | `bbbbbbbb-0000-0000-0000-000000000001` | Active |
| Cabin  | `bbbbbbbb-0000-0000-0000-000000000002` | Paused |
| Office | `bbbbbbbb-0000-0000-0000-000000000003` | Active |

There are 8 tasks (`cccccccc-…-000000000001` to `…08`), with daily, weekly, monthly, on-complete and inactive ones. There are also 12 assignments (`dddddddd-…`) in TODO, IN_PROGRESS and DONE, with due dates from 20 days ago to 4 days ahead. All dates are relative to when the seed ran. See [seed/seed.sql](seed/seed.sql).

The TaskRepeater is marked as having just run, so it won't add assignments for an hour. Creating or editing a task through the API triggers it right away.

```bash
TOKEN=$(curl -s -X POST localhost:8081/login -H 'Content-Type: application/json' \
  -d '{"id":"aaaaaaaa-0000-0000-0000-000000000002","key":"test-key"}' | jq -r .authToken)
curl -s -H "Authorization: Bearer $TOKEN" "localhost:8081/task-assignments/filter?progress=1&progress=2" | jq
```

Interactive CLI (pick a user, then login / view tasks / view TODO assignments / mark done / skip):

```bash
bash docker/chores-cli.sh
```

Inspect the DB: `docker compose -f docker/compose.yaml exec db psql -U postgres -d chores`

## Use docker database with IDE

Put following in main/resources/env.properties
```properties
# Docker postgres
DB_URL=jdbc:postgresql://localhost:5433/chores
DB_DRIVER=org.postgresql.Driver
DB_USERNAME=postgres
DB_PASSWORD=postgres
```