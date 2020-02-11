FROM openjdk:8
VOLUME /tmp
ADD build/libs/cx-flow-1.5.3.jar //
ENTRYPOINT ["/usr/local/openjdk-8/bin/java"]
CMD ["-Xms512m", "-Xmx2048m","-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "/cx-flow-1.5.3.jar"]
EXPOSE 8080:8080
