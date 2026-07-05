#!/usr/bin/env bash
# Apply a pending DDL file to the dev RDS via AWS SSM Run Command.
#
# The SQL file is base64-encoded to avoid shell-quoting issues, decoded on the
# EC2 host, and applied with psql running inside a postgres:16-alpine container
# (the host itself does not have psql installed). All statements run in a single
# transaction (ON_ERROR_STOP=1) so a partial/failed migration does not leave the
# dev schema half-applied.
#
# On success the ledger file is deleted automatically (it is gitignored, so there
# is nothing to commit). If the file is empty/missing this is a no-op success.
#
# Usage: bash apply-pending-ddl.sh docs/pending-ddl.sql
set -euo pipefail

DDL_FILE="${1:?Usage: apply-pending-ddl.sh <path-to-sql-file>}"
REGION="ap-northeast-2"

if [ ! -s "$DDL_FILE" ]; then
  echo "NO_PENDING_DDL: '$DDL_FILE' is empty or missing — no schema change this deploy."
  exit 0
fi

echo "=== Resolving target instance & DB credentials (SSM) ==="
INSTANCE_ID=$(aws ec2 describe-instances --region "$REGION" \
  --filters "Name=tag:Name,Values=recaring-app-server" "Name=instance-state-name,Values=running" \
  --query 'Reservations[0].Instances[0].InstanceId' --output text)

DB_HOST=$(aws ssm get-parameter --name "/recaring/DB_URL" --with-decryption --region "$REGION" \
  --query 'Parameter.Value' --output text | sed 's|jdbc:postgresql://||' | cut -d/ -f1 | cut -d: -f1)
DB_USER=$(aws ssm get-parameter --name "/recaring/DB_USERNAME" --with-decryption --region "$REGION" \
  --query 'Parameter.Value' --output text)
DB_PASS=$(aws ssm get-parameter --name "/recaring/DB_PASSWORD" --with-decryption --region "$REGION" \
  --query 'Parameter.Value' --output text)

echo "Instance: $INSTANCE_ID | DB host: $DB_HOST | DB user: $DB_USER"

# Encode the whole file so newlines / quotes survive the SSM command boundary.
SQL_B64=$(base64 < "$DDL_FILE" | tr -d '\n')

# On the host: decode to a temp file, apply via a throwaway psql container in one
# transaction, then clean up the temp file.
REMOTE="set -e; echo $SQL_B64 | base64 -d > /tmp/pending-ddl.sql; \
docker run --rm -e PGPASSWORD='$DB_PASS' -v /tmp/pending-ddl.sql:/tmp/pending-ddl.sql:ro postgres:16-alpine \
psql -v ON_ERROR_STOP=1 --single-transaction -h $DB_HOST -U $DB_USER -d recaring -f /tmp/pending-ddl.sql; \
rm -f /tmp/pending-ddl.sql"

echo "=== Sending DDL via SSM Run Command ==="
COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --region "$REGION" \
  --parameters "commands=[\"$REMOTE\"]" \
  --query 'Command.CommandId' --output text)

echo "CommandId: $COMMAND_ID"

# Poll until the command leaves InProgress (max ~60s).
for _ in $(seq 1 12); do
  sleep 5
  RESULT=$(aws ssm get-command-invocation \
    --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" --region "$REGION" \
    --query '{Status:Status,Output:StandardOutputContent,Error:StandardErrorContent}' \
    --output json)
  STATUS=$(printf '%s' "$RESULT" | sed -n 's/.*"Status": *"\([^"]*\)".*/\1/p')
  [ "$STATUS" = "InProgress" ] || { echo "$RESULT"; break; }
done

if [ "$STATUS" != "Success" ]; then
  echo "=== DDL APPLY FAILED (Status: $STATUS) — aborting before merge ===" >&2
  exit 1
fi

# Consume the ledger: schema is applied, so remove the local file (gitignored).
rm -f "$DDL_FILE"
echo "=== Status: Success — schema applied to dev, ledger '$DDL_FILE' removed ==="
