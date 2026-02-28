# ========== Stage 1: Maven 编译 ==========
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 先复制 pom.xml 利用 Docker 缓存层（依赖不变时不重新下载）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 再复制源码并打包
COPY src ./src
RUN mvn clean package -DskipTests -q

# ========== Stage 2: JRE 运行 ==========
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="zhulongqihan"
LABEL description="MyBlog Spring Boot Backend"

WORKDIR /app

# 从构建阶段复制 JAR
COPY --from=builder /build/target/blog-backend-1.0.0.jar app.jar

# 创建上传目录和日志目录
RUN mkdir -p /app/uploads /app/logs

# 暴露端口
EXPOSE 8080

# JVM 参数通过环境变量控制
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# 健康检查：启动后等60秒再开始检查，每30秒检查一次，失败5次才标记不健康
HEALTHCHECK --interval=30s --timeout=5s --retries=5 --start-period=60s \
  CMD wget -qO- http://localhost:8080/api/articles?page=0\&size=1 || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
