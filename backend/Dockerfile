# 构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 复制Maven配置
COPY settings.xml /usr/share/maven/ref/settings.xml

# 复制pom.xml并预下载依赖
COPY pom.xml .
RUN mvn -s /usr/share/maven/ref/settings.xml dependency:go-offline 2>/dev/null || true

# 复制源代码并编译
COPY src ./src
RUN mvn -s /usr/share/maven/ref/settings.xml clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制编译的jar文件
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8615

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
