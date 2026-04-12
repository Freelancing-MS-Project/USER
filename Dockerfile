FROM eclipse-temurin:17
COPY target/*.jar user.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "user.jar"]