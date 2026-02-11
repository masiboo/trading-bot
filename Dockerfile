# Build stage
FROM maven:3.9-eclipse-temurin-21 as builder

WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/target/trading-bot-1.0.0.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/trading/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
