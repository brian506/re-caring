#!/bin/bash
# 단위 테스트 포함 전체 빌드를 실행한다
# Usage: bash build-and-test.sh

set -e

echo "=== Starting clean build with tests ==="
./gradlew clean build

echo "=== Build & test passed ==="
