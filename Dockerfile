FROM alpine:3.19 AS java17

WORKDIR app
RUN apk update && \
    apk upgrade && \
    apk upgrade
RUN apk add openjdk17=17.0.13_p11-r0 --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
RUN apk add libstdc++
RUN apk add glib
RUN apk add krb5 pcre
RUN apk add bash
RUN wget "https://sca-downloads.s3.amazonaws.com/cli/latest/ScaResolver-musl64.tar.gz" -O "ScaResolver.tar.gz" && tar -xvzf ScaResolver.tar.gz && mv ScaResolver Configuration.yml /app && rm ScaResolver.tar.gz
COPY build/libs/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "cx-flow.jar"]
EXPOSE 8080

FROM debian:11 AS java17-debian

WORKDIR /app
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk
ENV JAVA_HOME /usr/lib/jvm/java-17-openjdk-amd64
ENV PATH $JAVA_HOME/bin:$PATH
RUN apt update
RUN apt install -y ca-certificates libgssapi-krb5-2
RUN apt install -y wget
RUN wget https://sca-downloads.s3.amazonaws.com/cli/latest/ScaResolver-linux64.tar.gz -O "ScaResolver.tar.gz" && tar -xvzf ScaResolver.tar.gz && rm ScaResolver.tar.gz
COPY build/libs/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "cx-flow.jar"]
EXPOSE 8080

FROM alpine:3.19 AS cxgo8

WORKDIR app
RUN apk update && \
    apk upgrade
RUN apk add openjdk8=8.302.08-r2 --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community
ENV JAVA_HOME=/usr/lib/jvm/java-1.8-openjdk
RUN apk add libstdc++
RUN apk add glib
RUN apk add krb5 pcre
RUN apk add bash
RUN wget "https://sca-downloads.s3.amazonaws.com/cli/latest/ScaResolver-musl64.tar.gz" -O "ScaResolver.tar.gz" && tar -xvzf ScaResolver.tar.gz && mv ScaResolver Configuration.yml /app && rm ScaResolver.tar.gz
COPY build/libs/cxgo/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=cxgo", "-jar", "cx-flow.jar"]
EXPOSE 8080