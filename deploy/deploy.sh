#!/usr/bin/env bash
# 서버에서 블로그를 도커로 (다시) 띄우는 스크립트.
# GitHub Actions가 도커 이미지(blog-dock.tar.gz)와 이 파일을 ~/blog-dock 에 올린 뒤 실행한다.
# 손으로 다시 띄울 때: cd ~/blog-dock && ./deploy.sh
set -euo pipefail

APP_DIR="$HOME/blog-dock"
IMAGE=blog-dock:latest
CONTAINER=blog-dock
cd "$APP_DIR"
mkdir -p uploads logs

# 접속 정보는 배포할 때 GitHub Secrets에서 받아 app.env에 저장한다 (나만 읽을 수 있게).
# 도커 --env-file 형식이라 값에 따옴표를 붙이지 않는다
if [ -n "${DB_ADDRESS:-}" ]; then
  umask 077
  {
    # 카카오 키를 Secrets에 넣었으면 카카오 로그인도 켠다
    if [ -n "${KAKAO_CLIENT_ID:-}" ]; then
      echo "SPRING_PROFILES_ACTIVE=prod,kakao"
      echo "KAKAO_CLIENT_ID=$KAKAO_CLIENT_ID"
      echo "KAKAO_CLIENT_SECRET=${KAKAO_CLIENT_SECRET:-}"
    else
      echo "SPRING_PROFILES_ACTIVE=prod"
    fi
    echo "APP_PORT=${APP_PORT:-8080}"
    echo "DB_ADDRESS=$DB_ADDRESS"
    echo "DB_PORT=${DB_PORT:-3306}"
    echo "DB_NAME=$DB_NAME"
    echo "DB_USERNAME=$DB_USERNAME"
    echo "DB_PASSWORD=$DB_PASSWORD"
  } > app.env
fi
if [ ! -f app.env ]; then
  echo "app.env가 없어요. GitHub Actions로 한 번 배포해 주세요." >&2
  exit 1
fi
APP_PORT=$(grep '^APP_PORT=' app.env | cut -d= -f2-)

# 예전에 java로 직접 띄운 블로그가 있으면 끈다 (도커로 옮기기 전 방식)
if [ -f app.pid ]; then
  if kill -0 "$(cat app.pid)" 2>/dev/null; then
    echo "예전 방식(java)으로 띄운 블로그 종료 (pid $(cat app.pid))"
    kill "$(cat app.pid)"
    for _ in $(seq 30); do kill -0 "$(cat app.pid)" 2>/dev/null || break; sleep 1; done
    kill -9 "$(cat app.pid)" 2>/dev/null || true
  fi
  rm -f app.pid
fi

if ! docker info > /dev/null 2>&1; then
  echo "이 계정으로 docker를 쓸 수 없어요. 서버에서 'docker ps'를 쳐서 나온 오류를 강사님께 보여 주세요." >&2
  exit 1
fi

# 새 이미지를 불러와서 컨테이너를 바꿔 끼운다
if [ -f blog-dock.tar.gz ]; then
  echo "도커 이미지 불러오는 중"
  gunzip -c blog-dock.tar.gz | docker load
fi
docker rm -f "$CONTAINER" > /dev/null 2>&1 || true
# 도커 기본 네트워크에서는 데이터베이스까지 길이 없어서(No route to host) 서버의 네트워크를 같이 쓴다.
# 그래서 컨테이너 안에서도 APP_PORT로 바로 연다
docker run -d --name "$CONTAINER" --restart unless-stopped \
  --user "$(id -u):$(id -g)" \
  --network host -e SERVER_PORT="$APP_PORT" \
  --env-file app.env \
  -v "$APP_DIR/uploads:/app/uploads" \
  "$IMAGE" > /dev/null
docker image prune -f > /dev/null 2>&1 || true
echo "블로그 컨테이너 시작 (포트 ${APP_PORT})"

# 켜질 때까지 기다렸다가 확인한다
for _ in $(seq 90); do
  if curl -fs "http://localhost:${APP_PORT}/api/health" > /dev/null; then
    echo "배포 성공: http://서버주소:${APP_PORT}/"
    exit 0
  fi
  if [ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)" != "true" ]; then break; fi
  sleep 2
done
echo "블로그가 켜지지 않았어요. 마지막 로그:" >&2
docker logs --tail 60 "$CONTAINER" >&2 || true
exit 1
