#!/usr/bin/env bash
# 서버에서 블로그를 (다시) 띄우는 스크립트.
# GitHub Actions가 app.jar와 이 파일을 ~/blog-dock 에 올린 뒤 실행한다.
# 손으로 다시 띄울 때: cd ~/blog-dock && ./deploy.sh
set -euo pipefail

APP_DIR="$HOME/blog-dock"
cd "$APP_DIR"
mkdir -p uploads logs

# 접속 정보는 처음 배포할 때 GitHub Secrets에서 받아 app.env에 저장한다 (나만 읽을 수 있게)
if [ -n "${DB_ADDRESS:-}" ]; then
  umask 077
  {
    # 카카오 키를 Secrets에 넣었으면 카카오 로그인도 켠다
    if [ -n "${KAKAO_CLIENT_ID:-}" ]; then
      printf 'SPRING_PROFILES_ACTIVE=prod,kakao\n'
      printf 'KAKAO_CLIENT_ID=%q\n' "$KAKAO_CLIENT_ID"
      printf 'KAKAO_CLIENT_SECRET=%q\n' "${KAKAO_CLIENT_SECRET:-}"
    else
      printf 'SPRING_PROFILES_ACTIVE=prod\n'
    fi
    printf 'SERVER_PORT=%q\n' "${APP_PORT:-8080}"
    printf 'DB_ADDRESS=%q\n' "$DB_ADDRESS"
    printf 'DB_PORT=%q\n' "${DB_PORT:-3306}"
    printf 'DB_NAME=%q\n' "$DB_NAME"
    printf 'DB_USERNAME=%q\n' "$DB_USERNAME"
    printf 'DB_PASSWORD=%q\n' "$DB_PASSWORD"
    printf 'UPLOAD_DIR=%q\n' "$APP_DIR/uploads"
  } > app.env
fi
if [ ! -f app.env ]; then
  echo "app.env가 없어요. GitHub Actions로 한 번 배포해 주세요." >&2
  exit 1
fi
set -a; . ./app.env; set +a

# java 찾기. ssh로 명령만 실행하면 로그인할 때 읽는 PATH 설정을 안 읽어서 java가 안 보일 수 있다
find_java() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then echo "$JAVA_HOME/bin/java"; return; fi
  if command -v java; then return; fi
  local c
  for c in "$(bash -lc 'command -v java' 2>/dev/null </dev/null)" \
           "$(bash -ic 'command -v java' 2>/dev/null </dev/null | tail -n 1)" \
           "$HOME"/.sdkman/candidates/java/current/bin/java \
           /usr/lib/jvm/*/bin/java /opt/*/bin/java /usr/local/*/bin/java "$HOME"/*/bin/java; do
    if [ -x "$c" ]; then echo "$c"; return; fi
  done
  return 1
}
if ! JAVA=$(find_java); then
  echo "서버에서 java를 찾지 못했어요. 서버 터미널에서 'which java'를 쳐서 나온 경로를 알려 주세요." >&2
  exit 1
fi
echo "java: $JAVA"

# 이전에 띄운 블로그를 끈다
if [ -f app.pid ] && kill -0 "$(cat app.pid)" 2>/dev/null; then
  echo "이전 블로그 종료 (pid $(cat app.pid))"
  kill "$(cat app.pid)"
  for _ in $(seq 30); do kill -0 "$(cat app.pid)" 2>/dev/null || break; sleep 1; done
  kill -9 "$(cat app.pid)" 2>/dev/null || true
fi

# 새로 띄운다. 터미널을 닫아도 계속 돌도록 nohup으로 실행한다
nohup "$JAVA" -Xmx512m -jar app.jar > logs/app.log 2>&1 &
echo $! > app.pid
echo "블로그 시작 (pid $(cat app.pid), 포트 ${SERVER_PORT})"

# 켜질 때까지 기다렸다가 확인한다
for _ in $(seq 90); do
  if curl -fs "http://localhost:${SERVER_PORT}/api/health" > /dev/null; then
    echo "배포 성공: http://서버주소:${SERVER_PORT}/"
    exit 0
  fi
  if ! kill -0 "$(cat app.pid)" 2>/dev/null; then break; fi
  sleep 2
done
echo "블로그가 켜지지 않았어요. 마지막 로그:" >&2
tail -n 60 logs/app.log >&2
exit 1
