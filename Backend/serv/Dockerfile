
# Build stage
FROM eclipse-temurin:26-jdk AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline

COPY src/ src/

RUN ./mvnw -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:26-jre

WORKDIR /app

COPY --from=build /app/target/serv-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-jar", "app.jar"]
