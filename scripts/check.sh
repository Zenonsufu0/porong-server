#!/usr/bin/env bash
set -euo pipefail

if [ -f package.json ]; then
  echo "[check] Node 프로젝트 감지"
  npm run lint
  npm test
elif [ -f gradlew ]; then
  echo "[check] Gradle 프로젝트 감지"
  ./gradlew test
elif [ -f pom.xml ]; then
  echo "[check] Maven 프로젝트 감지"
  mvn test
else
  echo "[check] 자동 검증 명령을 찾지 못함"
  echo "scripts/check.sh를 프로젝트에 맞게 수정하세요."
  exit 1
fi
