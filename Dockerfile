FROM alpine:3.17 AS java8

WORKDIR app
RUN apk update && \
    apk upgrade && \
    apk upgrade
RUN apk add openjdk8=8.345.01-r3 --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community
ENV JAVA_HOME=/usr/lib/jvm/java-1.8-openjdk
RUN apk --no-cache add curl
RUN apk add sudo
RUN apk add libstdc++
RUN apk add glib
RUN apk add krb5 pcre
RUN apk add bash
RUN curl -L "https://sca-downloads.s3.amazonaws.com/cli/latest/ScaResolver-musl64.tar.gz" -o "ScaResolver.tar.gz" && tar -vxzf ScaResolver.tar.gz && sudo mv ScaResolver Configuration.ini /app && rm ScaResolver.tar.gz
COPY build/libs/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "cx-flow.jar"]
EXPOSE 8080


FROM alpine:3.17 AS java11

WORKDIR app
RUN apk update && \
    apk upgrade 
RUN apk add openjdk11=11.0.17_p8-r1 --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk
RUN apk --no-cache add curl
RUN apk add sudo
RUN apk add libstdc++
RUN apk add glib
RUN apk add krb5 pcre
RUN apk add bash
RUN curl -L "https://sca-downloads.s3.amazonaws.com/cli/latest/ScaResolver-musl64.tar.gz" -o "ScaResolver.tar.gz" && tar -vxzf ScaResolver.tar.gz && sudo mv ScaResolver Configuration.ini /app && rm ScaResolver.tar.gz
COPY build/libs/java11/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m","-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=web", "-jar", "cx-flow.jar"]
EXPOSE 8080

FROM alpine:3.17 AS cxgo8

WORKDIR app
RUN apk update && \
    apk upgrade
RUN apk add openjdk8=8.302.08-r2 --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community
ENV JAVA_HOME=/usr/lib/jvm/java-1.8-openjdk
RUN apk --no-cache add curl
RUN apk add sudo
RUN apk add libstdc++
RUN apk add glib
RUN apk add krb5 pcre
RUN apk add bash
RUN curl -L "https://sca-downloads.s3.amazonaws.com/cli/latest/ScaResolver-musl64.tar.gz" -o "ScaResolver.tar.gz" && tar -vxzf ScaResolver.tar.gz && sudo mv ScaResolver Configuration.ini /app && rm ScaResolver.tar.gz
COPY build/libs/cxgo/*.jar cx-flow.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=cxgo", "-jar", "cx-flow.jar"]
EXPOSE 8080
