FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

COPY target/der-kitchen-server.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
