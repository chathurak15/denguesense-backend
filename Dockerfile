FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /opt/tomcat/logs
COPY --from=build /app/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Colombo
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Colombo"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
