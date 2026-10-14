#!/usr/bin/env bash
# 서버에서 블로그를 도커로 (다시) 띄우는 스크립트. 롤링 배포를 한다.
# 블로그를 두 개(APP_PORT, APP_PORT_2) 띄워 두고 하나씩 새 버전으로 바꾼다.
# 하나를 바꾸는 동안 다른 하나가 손님을 받아서 사이트가 끊기지 않는다 (nginx가 두 포트를 다 가리킬 때).
# GitHub Actions가 도커 이미지(seohyun-blog.tar.gz)와 이 파일을 ~/blog-dock 에 올린 뒤 실행한다.
# 손으로 다시 띄울 때: cd ~/blog-dock && ./deploy.sh
set -euo pipefail

APP_DIR="$HOME/blog-dock"
IMAGE=seohyun-blog:latest
CONTAINER=seohyun-blog      # 1번 블로그 (APP_PORT)
CONTAINER_2=seohyun-blog-2  # 2번 블로그 (APP_PORT_2)
cd "$APP_DIR"
mkdir -p uploads logs

# 접속 정보는 배포할 때 GitHub Secrets에서 받아 app.env에 저장한다 (나만 읽을 수 있게).
# 도커 --env-file 형식이라 값에 따옴표를 붙이지 않는다
if [ -n "${DB_ADDRESS:-}" ]; then
  umask 077
  {
    # 카카오·구글·깃허브 키를 Secrets에 넣었으면 그 로그인도 켠다
    PROFILES=prod
    if [ -n "${KAKAO_CLIENT_ID:-}" ]; then
      PROFILES="$PROFILES,kakao"
      echo "KAKAO_CLIENT_ID=$KAKAO_CLIENT_ID"
      echo "KAKAO_CLIENT_SECRET=${KAKAO_CLIENT_SECRET:-}"
    fi
    if [ -n "${GOOGLE_CLIENT_ID:-}" ]; then
      PROFILES="$PROFILES,google"
      echo "GOOGLE_CLIENT_ID=$GOOGLE_CLIENT_ID"
      echo "GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET:-}"
    fi
    if [ -n "${GH_CLIENT_ID:-}" ]; then
      PROFILES="$PROFILES,github"
      echo "GH_CLIENT_ID=$GH_CLIENT_ID"
      echo "GH_CLIENT_SECRET=${GH_CLIENT_SECRET:-}"
    fi
    echo "SPRING_PROFILES_ACTIVE=$PROFILES"
    echo "APP_PORT=${APP_PORT:-8080}"
    echo "APP_PORT_2=${APP_PORT_2:-$(( ${APP_PORT:-8080} + 1 ))}"
    echo "DB_ADDRESS=$DB_ADDRESS"
    echo "DB_PORT=${DB_PORT:-3306}"
    echo "DB_NAME=$DB_NAME"
    echo "DB_USERNAME=$DB_USERNAME"
    echo "DB_PASSWORD=$DB_PASSWORD"
    # 관리자(아이디 admin) 비밀번호. Secrets에 넣었을 때만
    if [ -n "${ADMIN_PASSWORD:-}" ]; then
      echo "ADMIN_PASSWORD=$ADMIN_PASSWORD"
    fi
  } > app.env
fi
if [ ! -f app.env ]; then
  echo "app.env가 없어요. GitHub Actions로 한 번 배포해 주세요." >&2
  exit 1
fi
APP_PORT=$(grep '^APP_PORT=' app.env | cut -d= -f2-)
APP_PORT_2=$(grep '^APP_PORT_2=' app.env | cut -d= -f2- || true)
APP_PORT_2=${APP_PORT_2:-$((APP_PORT + 1))}

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
if [ -f seohyun-blog.tar.gz ]; then
  echo "도커 이미지 불러오는 중"
  gunzip -c seohyun-blog.tar.gz | docker load
fi
docker rm -f blog-dock > /dev/null 2>&1 || true  # 이름 바꾸기 전 컨테이너

# 다른 학생들 블로그처럼 -p 포트:8080 으로 열어야 학교 안 다른 컴퓨터에서 들어올 수 있다.
# 그런데 컨테이너 네트워크에서 데이터베이스까지 길이 없으면(No route to host) 블로그가 켜지지 않는다.
# 데이터베이스 주소가 이 서버의 다른 도커 네트워크 주소 범위와 겹치면 그렇게 되므로,
# 겹치지 않는 우리 전용 네트워크를 만들어 먼저 시도하고, 그래도 안 닿을 때만 서버 네트워크를 같이 쓴다.
DB_ADDRESS=$(grep '^DB_ADDRESS=' app.env | cut -d= -f2-)
DB_PORT=$(grep '^DB_PORT=' app.env | cut -d= -f2-)
DB_IP=$(getent ahostsv4 "$DB_ADDRESS" | awk 'NR==1{print $1}')
echo "데이터베이스로 가는 길: $(ip route get "$DB_IP" 2>/dev/null | grep -o 'dev [^ ]*' || echo '알 수 없음')"

can_reach_db() {  # $1 = 도커 네트워크 이름
  docker run --rm --network "$1" --entrypoint bash "$IMAGE" \
    -c "timeout 8 bash -c 'exec 3<>/dev/tcp/${DB_IP:-$DB_ADDRESS}/${DB_PORT}'" 2>/dev/null
}

NET=blog-dock-net
if ! docker network inspect "$NET" > /dev/null 2>&1; then
  for SUBNET in 192.168.238.0/24 10.238.38.0/24 172.31.238.0/24; do
    case "$DB_IP" in "${SUBNET%.0/24}".*) continue;; esac
    docker network create --subnet "$SUBNET" "$NET" > /dev/null 2>&1 && { echo "도커 네트워크 $NET 만듦 ($SUBNET)"; break; }
  done
fi

if docker network inspect "$NET" > /dev/null 2>&1 && can_reach_db "$NET"; then
  echo "$NET 에서 데이터베이스 연결 확인 → -p 포트:8080 으로 엽니다"
  NETWORK_MODE=net
elif can_reach_db bridge; then
  echo "기본 네트워크에서 데이터베이스 연결 확인 → -p 포트:8080 으로 엽니다"
  NETWORK_MODE=bridge
else
  echo "컨테이너 네트워크에서는 데이터베이스에 닿지 않아요 → 서버 네트워크를 같이 씁니다 (학교 안에서 접속이 막힐 수 있어요)"
  NETWORK_MODE=host
fi

# 블로그 하나를 새 버전으로 바꾸고, 켜질 때까지 기다린다. $1 = 컨테이너 이름, $2 = 포트
replace_blog() {
  local name=$1 port=$2
  local network_args
  case "$NETWORK_MODE" in
    net)    network_args=(--network "$NET" -p "${port}:8080") ;;
    bridge) network_args=(-p "${port}:8080") ;;
    host)   network_args=(--network host -e SERVER_PORT="$port") ;;
  esac

  echo "[$name] 끄는 중 (처리 중인 요청은 마저 끝냄)"
  docker stop -t 30 "$name" > /dev/null 2>&1 || true
  docker rm -f "$name" > /dev/null 2>&1 || true

  docker run -d --name "$name" --restart unless-stopped \
    --user "$(id -u):$(id -g)" \
    "${network_args[@]}" \
    --env-file app.env \
    -v "$APP_DIR/uploads:/app/uploads" \
    "$IMAGE" > /dev/null
  echo "[$name] 새 버전 시작 (포트 ${port})"

  for _ in $(seq 90); do
    if curl -fs "http://localhost:${port}/api/health" > /dev/null; then
      echo "[$name] 켜짐: http://서버주소:${port}/"
      return 0
    fi
    if [ "$(docker inspect -f '{{.State.Running}}' "$name" 2>/dev/null)" != "true" ]; then break; fi
    sleep 2
  done
  echo "[$name] 블로그가 켜지지 않았어요. 마지막 로그:" >&2
  docker logs --tail 60 "$name" >&2 || true
  return 1
}

# 롤링 배포: 2번을 먼저 바꾸고, 2번이 잘 켜지면 1번을 바꾼다.
# 2번이 안 켜지면 여기서 멈춘다. 1번은 옛 버전 그대로라 사이트는 계속 열린다.
if ! replace_blog "$CONTAINER_2" "$APP_PORT_2"; then
  echo "배포 중단: 새 버전이 켜지지 않아서 1번 블로그(포트 ${APP_PORT})는 옛 버전으로 두었어요." >&2
  exit 1
fi
replace_blog "$CONTAINER" "$APP_PORT"

docker rmi blog-dock:latest > /dev/null 2>&1 || true; rm -f blog-dock.tar.gz  # 이름 바꾸기 전 이미지
docker image prune -f > /dev/null 2>&1 || true
echo "배포 성공: 블로그 두 개(포트 ${APP_PORT}, ${APP_PORT_2})가 새 버전이에요."
