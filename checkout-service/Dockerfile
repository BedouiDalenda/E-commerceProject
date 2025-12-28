FROM  eclipse-temurin:17-jdk
WORKDIR /app
COPY target/checkout-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]