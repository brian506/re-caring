#!/usr/bin/env bash
# 최초 1회 실행. ECR 생성 → IAM 역할 생성 → SSM 파라미터 등록 →
# 이미지 빌드/푸시 → 태스크 정의 등록 → ECS 서비스 생성
set -euo pipefail

REGION="ap-northeast-2"
ACCOUNT_ID="757052694924"
CLUSTER="recaring-cluster"
ECR_REPO="recaring-rule-base"
TASK_FAMILY="recaring-rule-base-task"
SERVICE_NAME="rule-base-service"
TASK_ROLE="recaring-rule-base-task-role"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ──────────────────────────────────────────────
# 사전 확인
# ──────────────────────────────────────────────
echo "=== [1/6] 환경 변수 확인 ==="
: "${GPS_QUEUE_URL:?GPS_QUEUE_URL 환경변수를 설정하세요 (예: export GPS_QUEUE_URL=https://sqs...)}"
: "${ALERT_QUEUE_URL:?ALERT_QUEUE_URL 환경변수를 설정하세요 (예: export ALERT_QUEUE_URL=https://sqs...)}"

# ──────────────────────────────────────────────
# ECR 리포지토리 생성
# ──────────────────────────────────────────────
echo "=== [2/6] ECR 리포지토리 생성 ==="
aws ecr describe-repositories --repository-names "$ECR_REPO" --region "$REGION" > /dev/null 2>&1 || \
  aws ecr create-repository \
    --repository-name "$ECR_REPO" \
    --region "$REGION" \
    --image-scanning-configuration scanOnPush=true \
    --query "repository.repositoryUri" --output text
echo "ECR: ${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${ECR_REPO}"

# ──────────────────────────────────────────────
# IAM 태스크 역할 생성 (SQS 수신·삭제·발행 권한)
# ──────────────────────────────────────────────
echo "=== [3/6] IAM 태스크 역할 생성 ==="
TRUST_POLICY='{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Service": "ecs-tasks.amazonaws.com" },
    "Action": "sts:AssumeRole"
  }]
}'

aws iam get-role --role-name "$TASK_ROLE" > /dev/null 2>&1 || \
  aws iam create-role \
    --role-name "$TASK_ROLE" \
    --assume-role-policy-document "$TRUST_POLICY" \
    --query "Role.RoleName" --output text

# SQS 인라인 정책 (GPS 큐 읽기·삭제, Alert 큐 발행)
SQS_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": "sqs:SendMessage",
      "Resource": "*"
    }
  ]
}
EOF
)
aws iam put-role-policy \
  --role-name "$TASK_ROLE" \
  --policy-name "rule-base-sqs-policy" \
  --policy-document "$SQS_POLICY"
echo "IAM 역할 준비 완료: $TASK_ROLE"

# ──────────────────────────────────────────────
# SSM 파라미터 등록
# ──────────────────────────────────────────────
echo "=== [4/6] SSM 파라미터 등록 ==="
aws ssm put-parameter \
  --name "/recaring/rule-base/gps-queue-url" \
  --value "$GPS_QUEUE_URL" \
  --type "String" \
  --overwrite \
  --region "$REGION"

aws ssm put-parameter \
  --name "/recaring/rule-base/alert-queue-url" \
  --value "$ALERT_QUEUE_URL" \
  --type "String" \
  --overwrite \
  --region "$REGION"
echo "SSM 파라미터 등록 완료"

# ──────────────────────────────────────────────
# 이미지 빌드 & ECR 푸시
# ──────────────────────────────────────────────
echo "=== [5/6] Docker 이미지 빌드 & 푸시 ==="
ECR_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${ECR_REPO}"

aws ecr get-login-password --region "$REGION" | \
  docker login --username AWS --password-stdin "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

docker build \
  --platform linux/amd64 \
  -t "${ECR_URI}:latest" \
  "$SCRIPT_DIR/../anomaly-detector"

docker push "${ECR_URI}:latest"
echo "이미지 푸시 완료: ${ECR_URI}:latest"

# ──────────────────────────────────────────────
# 태스크 정의 등록
# ──────────────────────────────────────────────
echo "=== [6/6] 태스크 정의 등록 & 서비스 생성 ==="
TASK_DEF_ARN=$(aws ecs register-task-definition \
  --cli-input-json "file://$SCRIPT_DIR/../anomaly-detector/task-definition.json" \
  --region "$REGION" \
  --query "taskDefinition.taskDefinitionArn" \
  --output text)
echo "태스크 정의 ARN: $TASK_DEF_ARN"

# 서비스가 이미 있으면 업데이트, 없으면 생성
if aws ecs describe-services \
     --cluster "$CLUSTER" \
     --services "$SERVICE_NAME" \
     --region "$REGION" \
     --query "services[0].status" --output text 2>/dev/null | grep -q "ACTIVE"; then
  aws ecs update-service \
    --cluster "$CLUSTER" \
    --service "$SERVICE_NAME" \
    --task-definition "$TASK_FAMILY" \
    --region "$REGION" \
    --query "service.serviceName" --output text
  echo "서비스 업데이트 완료: $SERVICE_NAME"
else
  aws ecs create-service \
    --cluster "$CLUSTER" \
    --service-name "$SERVICE_NAME" \
    --task-definition "$TASK_FAMILY" \
    --desired-count 1 \
    --launch-type EC2 \
    --scheduling-strategy REPLICA \
    --deployment-configuration "minimumHealthyPercent=0,maximumPercent=100" \
    --region "$REGION" \
    --query "service.serviceName" --output text
  echo "서비스 생성 완료: $SERVICE_NAME"
fi

echo ""
echo "완료. 컨테이너 상태 확인:"
echo "  aws ecs describe-services --cluster $CLUSTER --services $SERVICE_NAME --region $REGION"
