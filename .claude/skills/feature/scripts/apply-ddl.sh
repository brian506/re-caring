#!/usr/bin/env bash
# Apply DDL to the production RDS via AWS SSM Run Command.
# Usage: bash apply-ddl.sh "CREATE TABLE ..."
set -euo pipefail

DDL="${1:?Usage: apply-ddl.sh \"<DDL statement>\"}"

INSTANCE_ID=$(aws ec2 describe-instances \
  --region ap-northeast-2 \
  --filters "Name=tag:Name,Values=recaring-app-server" "Name=instance-state-name,Values=running" \
  --query 'Reservations[0].Instances[0].InstanceId' \
  --output text)

DB_HOST=$(aws ssm get-parameter --name "/recaring/DB_URL" --with-decryption \
  --region ap-northeast-2 --query 'Parameter.Value' --output text \
  | sed 's|jdbc:postgresql://||' | cut -d: -f1)
DB_USER=$(aws ssm get-parameter --name "/recaring/DB_USERNAME" --with-decryption \
  --region ap-northeast-2 --query 'Parameter.Value' --output text)
DB_PASS=$(aws ssm get-parameter --name "/recaring/DB_PASSWORD" --with-decryption \
  --region ap-northeast-2 --query 'Parameter.Value' --output text)

COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --region ap-northeast-2 \
  --parameters "commands=[\"PGPASSWORD=$DB_PASS psql -h $DB_HOST -U $DB_USER -d recaring -c '$DDL'\"]" \
  --query 'Command.CommandId' \
  --output text)

echo "CommandId: $COMMAND_ID — waiting 5s..."
sleep 5

aws ssm get-command-invocation \
  --command-id "$COMMAND_ID" \
  --instance-id "$INSTANCE_ID" \
  --region ap-northeast-2 \
  --query '{Status:Status,Output:StandardOutputContent,Error:StandardErrorContent}' \
  --output json
