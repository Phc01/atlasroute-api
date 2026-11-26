# AtlasRoute API

Sistema de logística em Java/Spring Boot com otimização de rotas e distribuição de cargas.

> Em construção — etapa 1 (base do projeto).

## Stack

- Java 21, Spring Boot 3.5
- PostgreSQL 17 + Flyway
- Swagger / OpenAPI (springdoc)
- JUnit 5, Testcontainers

## Como rodar

Pré-requisitos: JDK 21, Maven (ou o IntelliJ, que já traz um) e Docker.

```bash
# 1. sobe o banco
docker compose up -d

# 2. sobe a API
mvn spring-boot:run
```

- Swagger: http://localhost:8080/swagger-ui.html
- Status: http://localhost:8080/api/v1/status
- Health: http://localhost:8080/actuator/health

## Testes

```bash
mvn test
```

Os testes de integração sobem um PostgreSQL temporário com Testcontainers, então o Docker precisa estar rodando.
