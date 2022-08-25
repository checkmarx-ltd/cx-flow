FROM alpine:3.16 AS java8

WORKDIR app
RUN apk update && \
    apk upgrade && \
    apk upgrade
RUN apk add openjdk8=8.345.01-r0 --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community
ENV JAVA_HOME=/usr/lib/jvm/java-1.8-openjdk
COPY build/libs/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "cx-flow.jar"]
EXPOSE 8080


FROM alpine:3.16 AS java11

WORKDIR app
RUN apk update && \
    apk upgrade 
RUN apk add openjdk11=11.0.16.1_p1-r0 --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk
COPY build/libs/java11/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m","-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "cx-flow.jar"]
EXPOSE 8080

FROM alpine:3.16 AS cxgo8

WORKDIR app
RUN apk update && \
    apk upgrade
RUN apk add openjdk8=8.302.08-r2 --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community
ENV JAVA_HOME=/usr/lib/jvm/java-1.8-openjdk
COPY build/libs/cxgo/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=cxgo", "-jar", "cx-flow.jar"]
EXPOSE 8080
