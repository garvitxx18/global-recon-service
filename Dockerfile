FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system recon && useradd --system --gid recon recon
COPY --from=build /src/build/libs/global-recon-service-0.1.0.jar app.jar
USER recon
ENV PORT=8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT}"]
