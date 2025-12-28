FROM  eclipse-temurin:17-jdk
WORKDIR /app
COPY target/order-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]