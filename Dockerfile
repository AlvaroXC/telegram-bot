FROM eclipse-temurin:21-jdk-jammy
ARG JAR_FILE=target/facturaBot-0.0.1.jar
COPY ${JAR_FILE} app_facturaBot.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app_facturaBot.jar"]