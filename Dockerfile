FROM maven:3.8.6-jdk-11 AS build

#Build stage

WORKDIR /opt/app

COPY ./ /opt/app

RUN mvn clean install -DskipTests

#Docker build stage

FROM amazoncorretto:11-alphine3.13

COPY --from=build /opt/app/target/*.jar app.jar

ENV PORT 8081

EXPOSE $PORT

ENTRYPOINT ["java","-jar","-Xmx1024M","-Dserver.port=${PORT}","app.jar"]
