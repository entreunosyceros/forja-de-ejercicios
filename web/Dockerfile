FROM maven:3.9-eclipse-temurin-21 AS construccion
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package
# Construir la imagen
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=construccion /build/target/forjaexamenes-web-1.0.0.jar aplicacion.jar
RUN mkdir -p /app/examenes
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "aplicacion.jar"]
