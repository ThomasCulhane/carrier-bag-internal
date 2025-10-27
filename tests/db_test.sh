#!/usr/bin/env bash
# Basic CRUD integration tests for /api/rows
set -euo pipefail

BASE="${BASE:-http://localhost:80/api/rows}"
echo "Running API tests against: $BASE"

# helper to get HTTP code
http_code() {
  curl -sS -o /dev/null -w '%{http_code}' "$1"
}

# 1) LIST
echo "-> GET list"
code=$(http_code "$BASE")
if [ "$code" -ne 200 ]; then
  echo "GET list failed: HTTP $code" >&2
  exit 2
fi
echo "GET list OK"

# 2) CREATE
echo "-> POST create"
CREATE_RESP=$(curl -sS -X POST "$BASE" -H "Content-Type: application/json" -d '{
  "author":"test-suite",
  "title":"created-by-tests",
  "genre":"FICTION",
  "submitDate":"2025-10-22",
  "status":"UNASSIGNED",
  "emailed":false
}')
# extract numeric id
ID=$(echo "$CREATE_RESP" | sed -n 's/.*"id":[[:space:]]*\([0-9][0-9]*\).*/\1/p' || true)
if [ -z "$ID" ]; then
  echo "Create failed, response: $CREATE_RESP" >&2
  exit 3
fi
echo "Created id=$ID"

# 3) GET single
echo "-> GET single"
code=$(http_code "$BASE/$ID")
if [ "$code" -ne 200 ]; then
  echo "GET single failed: HTTP $code" >&2
  exit 4
fi
echo "GET single OK"

# 4) UPDATE
echo "-> PUT update (set emailed=true)"
PUT_RESP=$(curl -sS -X PUT "$BASE/$ID" -H "Content-Type: application/json" -d '{"emailed":true}')
UPDATED=$(echo "$PUT_RESP" | sed -n 's/.*"updated":[[:space:]]*\([0-9][0-9]*\).*/\1/p' || true)
if [ "$UPDATED" != "1" ]; then
  echo "PUT failed, response: $PUT_RESP" >&2
  exit 5
fi
echo "PUT OK"

# 5) DELETE
echo "-> DELETE"
DEL_CODE=$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE "$BASE/$ID")
if [ "$DEL_CODE" -ne 204 ]; then
  echo "DELETE failed: HTTP $DEL_CODE" >&2
  exit 6
fi
echo "DELETE OK"

# 6) VERIFY DELETED
echo "-> GET after delete (expect 404)"
code=$(http_code "$BASE/$ID")
if [ "$code" -ne 404 ]; then
  echo "GET after delete expected 404, got $code" >&2
  exit 7
fi
echo "Verified deletion. All tests passed."

exit 0