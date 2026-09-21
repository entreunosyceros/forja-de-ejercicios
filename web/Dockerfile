FROM maven:3.9-eclipse-temurin-21 AS construccion
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
# python3: subprocesos generador/evaluador; el repo se monta en /forja (compose)
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 \
    && rm -rf /var/lib/apt/lists/*
COPY --from=construccion /build/target/forjaexamenes-web-1.0.0.jar aplicacion.jar
EXPOSE 8080
ENV FORJAEXAMENES_RAIZ=/forja
ENV FORJAEXAMENES_SERVER_ADDRESS=0.0.0.0
ENTRYPOINT ["java", "-jar", "aplicacion.jar"]
