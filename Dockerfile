# syntax=docker/dockerfile:1

# Build stage: compile and package the Spring Boot app inside Docker
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache dependencies first
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B -q -DskipTests clean package

# Runtime stage: lightweight JRE image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the Spring Boot executable jar produced in the build stage
COPY --from=build /workspace/target/*.jar /app/
RUN set -eux; \
	jar_file="$(find /app -maxdepth 1 -name "*.jar" ! -name "*original*" | head -n 1)"; \
	test -n "$jar_file"; \
	mv "$jar_file" /app/app.jar; \
	useradd -r -u 1001 spring; \
	chown -R spring:spring /app

EXPOSE 8080
USER spring
ENTRYPOINT ["java", "-jar", "app.jar"]

