FROM  maven:4.0.0-rc-5-eclipse-temurin-25 as build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

EXPOSE 1200

COPY --from=build /app/target/*.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
