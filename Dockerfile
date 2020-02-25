FROM openjdk:8-jre AS java8

WORKDIR app
RUN apt update && \
    apt upgrade -y
COPY build/libs/*.jar app/cx-flow.jar
ENTRYPOINT ["java"]
CMD ["-Xms512m", "-Xmx2048m","-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "app/cx-flow.jar"]
EXPOSE 8080


FROM openjdk:11-jre AS java11

WORKDIR app
RUN apt update && \
    apt upgrade -y
COPY build/libs/java11/*.jar app/cx-flow.jar
ENTRYPOINT ["java"]
CMD ["-Xms512m", "-Xmx2048m","-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "app/cx-flow.jar"]
EXPOSE 8080
