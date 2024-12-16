FROM gradle:7.3-jdk17 AS builder

WORKDIR /usr/src/app

COPY . .

RUN gradle build --no-daemon
