#!/usr/bin/env bash
# Lists `idle in transaction` sessions on the dev RDS via AWS SSM Run Command.
# Such sessions hold locks that make a Flyway migration hit lock_timeout and fail app startup,
# so run this right before merging a migration that touches tables the detection engine reads.
#
# Usage: bash check-idle-transactions.sh
# Prints NO_IDLE_IN_TRANSACTION and exits 0 when none exist; prints the sessions and exits 1 otherwise.
set -euo pipefail

REGION="ap-northeast-2"

INSTANCE_ID=$(aws ec2 describe-instances --region "$REGION" \
  --filters "Name=tag:Name,Values=recaring-app-server" "Name=instance-state-name,Values=running" \
  --query 'Reservations[0].Instances[0].InstanceId' --output text)

DB_HOST=$(aws ssm get-parameter --name "/recaring/DB_URL" --with-decryption --region "$REGION" \
  --query 'Parameter.Value' --output text | sed 's|jdbc:postgresql://||' | cut -d/ -f1 | cut -d: -f1)
DB_USER=$(aws ssm get-parameter --name "/recaring/DB_USERNAME" --with-decryption --region "$REGION" \
  --query 'Parameter.Value' --output text)
DB_PASS=$(aws ssm get-parameter --name "/recaring/DB_PASSWORD" --with-decryption --region "$REGION" \
  --query 'Parameter.Value' --output text)

SQL="SELECT pid, application_name, client_addr, now() - state_change AS idle_for FROM pg_stat_activity WHERE state = 'idle in transaction' AND datname = 'recaring'"
REMOTE="docker run --rm -e PGPASSWORD='$DB_PASS' postgres:17-alpine psql -h $DB_HOST -U $DB_USER -d recaring -At -F ' | ' -c \"$SQL\""
REMOTE_B64=$(printf '%s' "$REMOTE" | base64 | tr -d '\n')

COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --region "$REGION" \
  --parameters "commands=[\"echo $REMOTE_B64 | base64 -d | bash\"]" \
  --query 'Command.CommandId' --output text)

for _ in $(seq 1 12); do
  sleep 5
  STATUS=$(aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
    --region "$REGION" --query 'Status' --output text)
  [ "$STATUS" = "InProgress" ] || [ "$STATUS" = "Pending" ] || break
done

OUTPUT=$(aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
  --region "$REGION" --query 'StandardOutputContent' --output text)

if [ "$STATUS" != "Success" ]; then
  echo "=== CHECK FAILED (Status: $STATUS) ===" >&2
  aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
    --region "$REGION" --query 'StandardErrorContent' --output text >&2
  exit 1
fi

if [ -z "${OUTPUT//[[:space:]]/}" ]; then
  echo "NO_IDLE_IN_TRANSACTION"
  exit 0
fi

echo "=== idle in transaction sessions (pid | application | client | idle_for) ==="
echo "$OUTPUT"
exit 1
