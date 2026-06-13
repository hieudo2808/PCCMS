# Stage 1: Build the application
FROM amazoncorretto:25-alpine AS build
RUN apk add --no-cache maven
WORKDIR /app
COPY pom.xml .
# Optionally pre-fetch dependencies for faster rebuilds
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM amazoncorretto:25-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
