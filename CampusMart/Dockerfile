# ========== 阶段 1：构建 ==========
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先只拷贝 pom，让依赖层单独缓存：改业务代码时不会重新下载依赖
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再拷贝源码构建
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ========== 阶段 2：运行 ==========
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m"

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
