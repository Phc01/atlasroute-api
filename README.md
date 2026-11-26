# AtlasRoute API

Sistema de logística em Java/Spring Boot com otimização de rotas e distribuição de cargas.

## Stack

- Java 21, Spring Boot 3.5
- PostgreSQL 17 + Flyway
- Swagger / OpenAPI (springdoc)
- JUnit 5, Testcontainers

## Como rodar

Pré-requisitos: JDK 21 e Docker Desktop (aberto). Não precisa instalar o Maven: o `mvnw.cmd`
baixa o Maven 3.9.11 em `%USERPROFILE%\.m2\wrapper` na primeira execução.

```powershell
# 1. sobe o banco
docker compose up -d

# 2. sobe a API
.\mvnw.cmd spring-boot:run
```

- Swagger: http://localhost:8080/swagger-ui.html
- Status: http://localhost:8080/api/v1/status
- Health: http://localhost:8080/actuator/health

## Testes

```powershell
.\mvnw.cmd test
```

Os testes de integração usam Testcontainers e precisam do Docker Desktop rodando.

> **Nota sobre o Testcontainers:** a versão está fixada em `1.21.4` no `pom.xml`
> (`testcontainers.version`). A `1.21.3`, que vem com o Spring Boot 3.5.6, não funciona com o
> Docker Engine 29, que exige a API 1.44 ou mais nova. Não remova essa propriedade.
