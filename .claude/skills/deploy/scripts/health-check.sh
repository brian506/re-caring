#!/bin/bash
# Verifies the deployed app via ECS (nginx blocks /actuator externally with 403, so it cannot be probed from outside).
# Usage: bash health-check.sh [expected-image-tag]
#   expected-image-tag: first 8 chars of the merge commit SHA (same as SHORT_SHA in deploy.yml). Optional.
#
# Passes when all of the following hold:
#   1. recaring-service has a single PRIMARY deployment with rolloutState COMPLETED and running == desired
#   2. every running task's `recaring` container is RUNNING / HEALTHY (container health check hits actuator internally)
#   3. (if given) every running task uses the expected image tag
#   4. an authenticated API through nginx answers 401, proving routing nginx -> app works

CLUSTER="recaring-cluster"
SERVICE="recaring-service"
CONTAINER="recaring"
ROUTE_URL="https://re-caring.com/api/v1/notifications"
EXPECTED_TAG="${1:-}"
MAX_RETRY=20
INTERVAL=15

echo "=== Health check: cluster=$CLUSTER service=$SERVICE expectedTag=${EXPECTED_TAG:-any} ==="

check_once() {
  local deployments primary_count rollout running desired task_arns containers bad route_status

  deployments=$(aws ecs describe-services --cluster "$CLUSTER" --services "$SERVICE" \
    --query 'services[0].deployments' --output json 2>&1) || { echo "describe-services failed: $deployments"; return 1; }

  primary_count=$(echo "$deployments" | jq 'length')
  rollout=$(echo "$deployments" | jq -r '.[] | select(.status=="PRIMARY") | .rolloutState')
  running=$(echo "$deployments" | jq -r '.[] | select(.status=="PRIMARY") | .runningCount')
  desired=$(echo "$deployments" | jq -r '.[] | select(.status=="PRIMARY") | .desiredCount')
  echo "  deployments=$primary_count rollout=$rollout running=$running desired=$desired"
  if [ "$primary_count" != "1" ] || [ "$rollout" != "COMPLETED" ] || [ "$running" != "$desired" ] || [ "$desired" = "0" ]; then
    return 1
  fi

  task_arns=$(aws ecs list-tasks --cluster "$CLUSTER" --service-name "$SERVICE" --desired-status RUNNING \
    --query 'taskArns' --output text 2>&1) || { echo "list-tasks failed: $task_arns"; return 1; }
  if [ -z "$task_arns" ] || [ "$task_arns" = "None" ]; then
    echo "  no running tasks"
    return 1
  fi

  containers=$(aws ecs describe-tasks --cluster "$CLUSTER" --tasks $task_arns \
    --query "tasks[].containers[?name=='$CONTAINER'][]" --output json 2>&1) || { echo "describe-tasks failed: $containers"; return 1; }
  echo "$containers" | jq -r '.[] | "  container status=\(.lastStatus) health=\(.healthStatus) image=\(.image | split(":") | last)"'

  bad=$(echo "$containers" | jq --arg tag "$EXPECTED_TAG" \
    '[.[] | select(.lastStatus != "RUNNING" or .healthStatus != "HEALTHY" or ($tag != "" and (.image | split(":") | last) != $tag))] | length')
  if [ "$bad" != "0" ]; then
    return 1
  fi

  route_status=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$ROUTE_URL")
  echo "  route $ROUTE_URL -> $route_status (expected 401)"
  [ "$route_status" = "401" ]
}

for i in $(seq 1 $MAX_RETRY); do
  echo "[$i/$MAX_RETRY]"
  if check_once; then
    echo "=== Server is UP ==="
    exit 0
  fi
  echo "Retrying in ${INTERVAL}s..."
  sleep "$INTERVAL"
done

echo "=== Health check FAILED after $MAX_RETRY attempts ==="
exit 1
