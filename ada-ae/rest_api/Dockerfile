FROM maven:3.6.3-openjdk-11 AS builder

WORKDIR /api
COPY . /api
RUN mvn clean package 

FROM openjdk:14-slim
WORKDIR /root
COPY --from=builder /api/target/ada_rest_api.jar  /root/ada_rest_api.jar
COPY --from=builder /api/target/lib  /root/lib
EXPOSE 7002
ENTRYPOINT ["java", "-jar", "ada_rest_api.jar"]

