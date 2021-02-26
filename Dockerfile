FROM openjdk:11-jdk as build

WORKDIR /src

# Cache gradle wrapper and dependencies
COPY gradle gradle
COPY gradlew *.gradle ./
RUN ./gradlew --no-daemon build 2>/dev/null || true

# Build the actual project
COPY . .
RUN ./gradlew --no-daemon clean build \
    && ./gradlew --no-daemon -b build-11.gradle --build-cache assemble \
    && ./gradlew --no-daemon -q getVersion > build/libs/version.txt \
	&& mv build /build

FROM openjdk:8-jre-alpine AS java8

WORKDIR app
RUN apk update && \
    apk upgrade && \
    apk upgrade
COPY --from=build /build/libs/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "cx-flow.jar"]
EXPOSE 8080


FROM openjdk:11-jre-slim AS java11

WORKDIR app
RUN apt update && \
    apt upgrade -y
COPY --from=build /build/libs/java11/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m","-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "cx-flow.jar"]
EXPOSE 8080
