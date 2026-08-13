FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system sicrec && useradd --system --gid sicrec --home-dir /app sicrec \
    && mkdir -p /app/data /app/backups /app/documents \
    && chown -R sicrec:sicrec /app
COPY --from=build --chown=sicrec:sicrec /workspace/target/*.jar /app/sicrec.jar
USER sicrec
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/sicrec.jar"]
