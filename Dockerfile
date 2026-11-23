FROM ubuntu:24.04 AS native-build
RUN apt-get update && apt-get install -y --no-install-recommends gcc libc6-dev && rm -rf /var/lib/apt/lists/*
WORKDIR /src/c
COPY --from=memoria_c / /src/c/
RUN gcc -Wall -Wextra -O2 -fPIC -shared -o /libmemoria.so memoria.c

FROM maven:3.9-eclipse-temurin-21 AS java-build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --no-create-home appuser && mkdir -p /app/lib && chown -R appuser:appuser /app
WORKDIR /app
COPY --from=java-build /workspace/target/*.jar /app/app.jar
COPY --from=native-build /libmemoria.so /app/lib/libmemoria.so
ENV MEMORIA_NATIVE_PATH=/app/lib/libmemoria.so ASIGNADOR_MOTOR=native
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
