FROM alpine:3.11
VOLUME /tmp
RUN apk add openjdk8-jre && apk update && apk upgrade
ADD build/libs/cx-flow-1.5.3.jar //
ENTRYPOINT ["java"]
CMD ["-Xms512m", "-Xmx2048m","-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "/cx-flow-1.5.3.jar"]
EXPOSE 8080:8080
