# syntax=docker/dockerfile:1.6

# ---------- Stage 1: build ----------
# 用轩辕镜像加速拉 maven 基础镜像
FROM docker.xuanyuan.run/library/maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Maven 走阿里云镜像,依赖下载快 10 倍以上
COPY docker/maven-settings.xml /root/.m2/settings.xml

# 先只拷贝 pom.xml,利用 Docker layer 缓存,依赖不变就不重下
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再拷贝源码
COPY src ./src

# 构建可执行 jar
RUN mvn -B -q -DskipTests package && \
    cp target/*.jar app.jar

# ---------- Stage 2: runtime ----------
FROM docker.xuanyuan.run/library/eclipse-temurin:17-jre-alpine
WORKDIR /app

# 换成阿里 alpine 源,apk 快 10 倍;装 curl (健康检查) 和 tzdata (时区)
RUN sed -i 's|dl-cdn.alpinelinux.org|mirrors.aliyun.com|g' /etc/apk/repositories && \
    apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

COPY --from=build /workspace/app.jar app.jar

# JVM 参数针对 2G 机器调优:Xmx1g, 容器感知内存, G1GC
ENV JAVA_OPTS="-Xms256m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Duser.timezone=Asia/Shanghai"

EXPOSE 8080

# Actuator 健康检查
HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"UP"' || exit 1

# 用 sh -c 让 $JAVA_OPTS 展开
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=prod"]
