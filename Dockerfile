FROM openjdk:8-jre-alpine AS java8

WORKDIR app
RUN apk update && \
    apk upgrade && \
    apk upgrade
COPY build/libs/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "cx-flow.jar"]
EXPOSE 8080


FROM openjdk:11-jre-slim AS java11

WORKDIR app
RUN apt update && \
    apt upgrade -y
COPY build/libs/java11/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m","-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "cx-flow.jar"]
EXPOSE 8080

FROM openjdk:8-jre-alpine AS cxgo8

WORKDIR app
RUN apk update && \
    apk upgrade
COPY build/libs/cxgo/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=cxgo", "-jar", "cx-flow.jar"]
EXPOSE 8080
