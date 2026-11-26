# AtlasRoute API

Sistema de logística em Java/Spring Boot com otimização de rotas e distribuição de cargas.

## Stack

- Java 21, Spring Boot 3.5
- PostgreSQL 17 + Flyway
- Swagger / OpenAPI (springdoc)
- JUnit 5, Testcontainers

## Como rodar

Pré-requisitos: JDK 21, Maven e Docker.

```bash
# 1. sobe o banco
docker compose up -d

# 2. sobe a API
.\mvnw.cmd spring-boot:run
```

- Swagger: http://localhost:8080/swagger-ui.html
- Status: http://localhost:8080/api/v1/status
- Health: http://localhost:8080/actuator/health

## Testes

```bash
.\mvnw.cmd test
```
