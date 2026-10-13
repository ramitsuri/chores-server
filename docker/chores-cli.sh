#!/usr/bin/env bash
# Interactive CLI for testing the Chores server running in docker/compose.yaml.
# Usage: bash docker/chores-cli.sh            (server at localhost:8081)
#        BASE_URL=http://host:port bash docker/chores-cli.sh
# Requires: curl, jq. Works with macOS's default bash 3.2.

BASE_URL="${BASE_URL:-http://localhost:8081}"
KEY="test-key" # Every seeded member's login key

# Seeded members (see docker/seed/seed.sql)
USER_NAMES=("Admin (all access)" "Alice (write house)" "Bob (write own)" "Carol (write own, Office only)")
USER_IDS=(
  "aaaaaaaa-0000-0000-0000-000000000001"
  "aaaaaaaa-0000-0000-0000-000000000002"
  "aaaaaaaa-0000-0000-0000-000000000003"
  "aaaaaaaa-0000-0000-0000-000000000004"
)

USER_ID=""
USER_NAME=""
TOKEN=""
HOUSE_ID=""
HOUSE_NAME=""
RESP_FILE=$(mktemp)
trap 'rm -f "$RESP_FILE"' EXIT

PROGRESS_NAMES='{"0":"UNKNOWN","1":"TODO","2":"IN_PROGRESS","3":"DONE","4":"WONT_DO"}'
REPEAT_NAMES='{"0":"none","1":"day","2":"week","3":"month","4":"hour","5":"year","6":"on complete"}'
ACTIVE_NAMES='{"1":"active","2":"inactive","3":"paused"}'

for cmd in curl jq; do
  command -v "$cmd" >/dev/null || { echo "Missing required command: $cmd"; exit 1; }
done

# api METHOD PATH [JSON_BODY] -> sets HTTP_CODE, body in $RESP_FILE
api() {
  local method=$1 path=$2 body=${3:-}
  local args=(-s -o "$RESP_FILE" -w '%{http_code}' -X "$method" "$BASE_URL$path")
  [ -n "$TOKEN" ] && args+=(-H "Authorization: Bearer $TOKEN")
  [ -n "$body" ] && args+=(-H 'Content-Type: application/json' -d "$body")
  HTTP_CODE=$(curl "${args[@]}") || HTTP_CODE="000"
}

fail() {
  echo "  ✗ HTTP $HTTP_CODE $(head -c 300 "$RESP_FILE")"
}

select_user() {
  echo
  echo "Select a user:"
  PS3="User #: "
  select name in "${USER_NAMES[@]}"; do
    if [ -n "$name" ]; then
      USER_ID=${USER_IDS[$((REPLY - 1))]}
      USER_NAME=$name
      TOKEN=""
      HOUSE_ID=""
      HOUSE_NAME=""
      echo "→ $USER_NAME ($USER_ID)"
      return
    fi
  done
}

login() {
  TOKEN=""
  api POST /login "{\"id\":\"$USER_ID\",\"key\":\"$KEY\"}"
  if [ "$HTTP_CODE" = "200" ]; then
    TOKEN=$(jq -r .authToken "$RESP_FILE")
    echo "  ✓ Logged in as $USER_NAME"
  else
    fail
    return 1
  fi
}

ensure_login() {
  [ -n "$TOKEN" ] || login
}

# Lets the user pick one of their houses (from /sync). Remembers the choice until "Change house".
select_house() {
  [ -n "$HOUSE_ID" ] && return 0
  ensure_login || return 1
  api GET /sync
  [ "$HTTP_CODE" = "200" ] || { fail; return 1; }

  local ids=() names=()
  while IFS=$'\t' read -r id name status; do
    ids+=("$id")
    names+=("$name ($status)")
  done < <(jq -r --argjson s "$ACTIVE_NAMES" \
    '.associatedLists[] | [.id, .name, ($s[.status|tostring] // .status|tostring)] | @tsv' "$RESP_FILE")

  if [ ${#ids[@]} -eq 0 ]; then
    echo "  $USER_NAME doesn't belong to any house"
    return 1
  fi

  echo "Select a house:"
  PS3="House #: "
  select name in "${names[@]}"; do
    if [ -n "$name" ]; then
      HOUSE_ID=${ids[$((REPLY - 1))]}
      HOUSE_NAME=$name
      echo "→ $HOUSE_NAME"
      return 0
    fi
  done
}

# Prints {memberId: name} for members in the user's houses, used to show names instead of IDs
member_names_json() {
  api GET /sync
  if [ "$HTTP_CODE" = "200" ]; then
    jq -c '[.memberListAssociations[].member | {key: .id, value: .name}] | from_entries' "$RESP_FILE"
  else
    echo '{}'
  fi
}

view_tasks() {
  select_house || return
  local members
  members=$(member_names_json)
  api GET /tasks
  [ "$HTTP_CODE" = "200" ] || { fail; return; }
  echo
  echo "Tasks in $HOUSE_NAME:"
  jq -r --arg h "$HOUSE_ID" --argjson m "$members" --argjson r "$REPEAT_NAMES" --argjson s "$ACTIVE_NAMES" '
    ["NAME", "ASSIGNEE", "DUE", "REPEATS", "ROTATE", "STATUS"],
    (.[] | select(.houseId == $h) |
      [.name, ($m[.memberId] // .memberId), .dueDateTime,
       (if .repeatUnit == 0 then "no"
        elif .repeatUnit == 6 then "on complete"
        else "every \(.repeatValue) \($r[.repeatUnit|tostring])" end),
       (.rotateMember|tostring), ($s[.status|tostring] // (.status|tostring))])
    | @tsv' "$RESP_FILE" | column -t -s $'\t'
}

# Loads TODO assignments for the selected house into TODO_IDS / TODO_LABELS
load_todo_assignments() {
  TODO_IDS=()
  TODO_LABELS=()
  api GET "/task-assignments/filter?progress=1"
  [ "$HTTP_CODE" = "200" ] || { fail; return 1; }
  while IFS=$'\t' read -r id label; do
    TODO_IDS+=("$id")
    TODO_LABELS+=("$label")
  done < <(jq -r --arg h "$HOUSE_ID" '
    sort_by(.dueDateTime)[] | select(.task.houseId == $h) |
    [.id, "\(.task.name) — \(.member.name), due \(.dueDateTime)"] | @tsv' "$RESP_FILE")
}

view_todo_assignments() {
  select_house || return
  load_todo_assignments || return
  echo
  echo "TODO assignments in $HOUSE_NAME:"
  if [ ${#TODO_IDS[@]} -eq 0 ]; then
    echo "  (none)"
    return
  fi
  local i
  for i in "${!TODO_LABELS[@]}"; do
    echo "  $((i + 1))) ${TODO_LABELS[$i]}"
  done
}

# update_assignment STATUS_KEY STATUS_NAME
update_assignment() {
  local status_key=$1 status_name=$2
  select_house || return
  load_todo_assignments || return
  if [ ${#TODO_IDS[@]} -eq 0 ]; then
    echo "  No TODO assignments in $HOUSE_NAME"
    return
  fi

  local assignment_id=""
  echo "Mark which assignment as $status_name?"
  PS3="Assignment # (or 0 to cancel): "
  select label in "${TODO_LABELS[@]}"; do
    [ "$REPLY" = "0" ] && return
    if [ -n "$label" ]; then
      assignment_id=${TODO_IDS[$((REPLY - 1))]}
      break
    fi
  done

  api PUT /task-assignments "[{\"id\":\"$assignment_id\",\"progressStatus\":$status_key}]"
  [ "$HTTP_CODE" = "200" ] || { fail; return; }

  # The API echoes the requested IDs even when it didn't update anything (e.g. no permission),
  # so check the actual status afterwards.
  api GET "/task-assignments/filter?progress=$status_key"
  if [ "$HTTP_CODE" = "200" ] && jq -e --arg id "$assignment_id" 'any(.[]; .id == $id)' "$RESP_FILE" >/dev/null; then
    echo "  ✓ Marked as $status_name"
  else
    echo "  ✗ Not updated. $USER_NAME probably doesn't have write access to this assignment."
  fi
}

change_house() {
  HOUSE_ID=""
  HOUSE_NAME=""
  select_house
}

main_menu() {
  local options=(
    "Login"
    "View tasks in a house"
    "View TODO assignments in a house"
    "Mark assignment as done"
    "Skip assignment (won't do)"
    "Change house"
    "Switch user"
    "Quit"
  )
  while true; do
    echo
    echo "[$USER_NAME${HOUSE_NAME:+ @ $HOUSE_NAME}]"
    PS3="Command #: "
    select opt in "${options[@]}"; do
      case $REPLY in
        1) login ;;
        2) view_tasks ;;
        3) view_todo_assignments ;;
        4) update_assignment 3 "DONE" ;;
        5) update_assignment 4 "WONT_DO" ;;
        6) change_house ;;
        7) select_user ;;
        8) exit 0 ;;
        *) echo "Invalid choice"; continue ;;
      esac
      break
    done
  done
}

echo "Chores CLI → $BASE_URL"
select_user
main_menu
