# 블로그독 서버 이미지. GitHub Actions가 화면을 넣어 빌드한 jar(deploy/app.jar)를 담는다.
# 직접 만들 때: (frontend 빌드 → backend/src/main/resources/static 복사 → mvn package 후)
#   cp backend/target/backend-0.0.1-SNAPSHOT.jar deploy/app.jar && docker build -t blog-dock .
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY deploy/app.jar app.jar
ENV SERVER_PORT=8080 \
    UPLOAD_DIR=/app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
