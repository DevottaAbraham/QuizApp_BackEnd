# Stage 1: Build the application using Maven
FROM openjdk:21-jdk-slim AS builder
WORKDIR /app

# Copy the Maven wrapper and pom.xml to leverage Docker layer caching
# This step only re-runs if the pom.xml or wrapper files change
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
# Add execute permissions to the Maven wrapper
RUN chmod +x ./mvnw

# Copy the rest of your application's source code
COPY src ./src

# Build the application using the Maven wrapper. The result is a .jar file in /app/target/
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the final, smaller image
FROM openjdk:21-slim
WORKDIR /app
# Copy only the built application JAR from the 'builder' stage
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
