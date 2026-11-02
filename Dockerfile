FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml checkstyle.xml pmd-ruleset.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd -r -u 10001 recipehub
COPY --from=build /workspace/target/recipehub-enterprise-1.0.0.jar /app/app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
