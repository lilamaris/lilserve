FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system app && useradd --system --gid app app

WORKDIR /app

COPY --chown=app:app build/libs/reservation-service.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
