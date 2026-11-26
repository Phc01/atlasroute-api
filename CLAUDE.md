# AtlasRoute API — contexto para o Claude Code

Projeto pessoal de portfólio do Pedro: sistema de logística em Java/Spring Boot cujo diferencial é
otimizar rotas e distribuir cargas. Feito do zero, só inspirado no domínio de logística — não usar
nome, código, telas ou regras de nenhum sistema de empresa.

Plano completo (Claude Doc): https://claude.ai/artifact/QKSVDGLGerqrzac5BjAHuG
Repositório: https://github.com/Phc01/atlasroute-api

## Stack

- Java 21, Spring Boot 3.5.6, Maven
- PostgreSQL 17 (Docker Compose) + Flyway (`src/main/resources/db/migration`)
- Spring Data JPA com `ddl-auto: validate` — o schema é sempre do Flyway, nunca do Hibernate
- springdoc-openapi 2.8.13 (Swagger em `/swagger-ui.html`)
- Testes: JUnit 5, Mockito, MockMvc, Testcontainers (`@ServiceConnection`)
- Próximas etapas: Spring Security + JWT

## Ambiente (Windows)

- Comandos sempre via wrapper: `.\mvnw.cmd spring-boot:run`, `.\mvnw.cmd test`
- Antes de rodar a aplicação: `docker compose up -d` (Docker Desktop precisa estar aberto)
- `mvnw.cmd` é um wrapper simplificado escrito à mão (baixa o Maven 3.9.11 em
  `%USERPROFILE%\.m2\wrapper`). Na etapa 7 trocar pelo oficial com `.\mvnw.cmd wrapper:wrapper`
  (gera também o `mvnw` para Linux/GitHub Actions).
- `testcontainers.version` está fixado em 1.21.4 no `pom.xml`: a 1.21.3 que vem com o Boot 3.5.6
  não funciona com o Docker Engine 29 (API mínima 1.44). Não remover.
- Graphify: o repositório está indexado no Graphify (branch `main`). Para ver o grafo local:
  `graphify extract . --code-only` e `graphify export html`. A pasta `graphify-out/` não vai pro Git.

## Convenções de código

- Pacote base `com.atlasroute.api`, organizado por domínio (ex.: `status/`, `auth/`, `veiculo/`)
- DTOs como `record`; controllers finos, regra no service
- Rotas versionadas: `/api/v1/...`
- Data/hora via bean `Clock` (testável); nunca `Instant.now()` sem clock
- Testes no padrão AAA com comentários `// Arrange`, `// Act`, `// Assert`; nomes em português
  (`deveRetornarStatusUp`)
- Mensagens, nomes de domínio e docs em português

## Fluxo de versionamento (o Pedro quer treinar isso — seguir sempre)

- `main` é estável; nada é commitado direto nela
- Cada tarefa = uma issue no GitHub + uma branch a partir da `main` atualizada
- Prefixos: `feature/`, `fix/`, `chore/`, `test/`
- Commits em Conventional Commits: `feat(auth): ...`, `fix: ...`, `docs: ...`, `chore: ...`, `test: ...`
- Termina com PR contendo `Closes #N` → merge → apagar a branch
- Ao começar uma tarefa, lembrar o Pedro de criar a issue e a branch; ao terminar, sugerir a
  mensagem de commit e o texto do PR. Deixar o Pedro rodar os comandos git para ele praticar.

## Etapas do projeto

1. ✅ Base: Spring Boot, PostgreSQL no Docker Compose, Flyway, Swagger, testes (já na `main`)
2. Autenticação: cadastro e login com JWT, perfis ADMIN e MOTORISTA, testes
3. Cadastros: veículos, motoristas, clientes e pedidos, com validação e paginação
4. Romaneio: criar romaneio, adicionar pedidos, validar capacidade do veículo
5. Otimização de rota: vizinho mais próximo + 2-opt, Haversine, km antes e depois
6. Distribuição de carga: romaneios automáticos respeitando peso e volume (bin packing)
7. Acabamento: README com prints e explicação dos algoritmos, GitHub Actions rodando os testes
8. Opcional: front React + Leaflet com o mapa da rota; comparar com Google OR-Tools

## Modelo de dados planejado

| Entidade | Campos principais |
|---|---|
| Usuário | nome, e-mail, senha (hash), perfil (ADMIN / MOTORISTA) |
| Veículo | placa, capacidade de peso (kg), capacidade de volume (m³) |
| Motorista | nome, CNH, usuário vinculado |
| Cliente | nome, endereço, latitude, longitude |
| Pedido | cliente, peso, volume, status |
| Romaneio | veículo, motorista, data, status, distância total |
| Parada do romaneio | romaneio, pedido, ordem, distância até a próxima |

## Tarefas da etapa 2 (uma branch cada)

| # | Branch | Escopo |
|---|---|---|
| 0 | `chore/readme-mvnw` | README com `.\mvnw.cmd` + nota do Testcontainers; `graphify-out/` no `.gitignore`; este CLAUDE.md |
| 1 | `feature/tratamento-erros` | `@RestControllerAdvice` com `ProblemDetail` (validação, 404, genérico) |
| 2 | `feature/usuario-entidade` | Migration `V2` tabela `usuario`, entidade, enum `Perfil`, repository |
| 3 | `feature/auth-cadastro` | `POST /api/v1/auth/register`, BCrypt, validações, testes |
| 4 | `feature/auth-jwt-login` | Geração/validação de JWT + `POST /api/v1/auth/login` |
| 5 | `feature/security-config` | Rotas públicas (auth, Swagger, health), resto protegido, perfis, 401/403 em JSON, "Authorize" no Swagger |
| 6 | `test/auth-integracao` | Fluxo completo com Testcontainers: cadastrar → logar → rota protegida |

## Estado atual

- Etapa 1 concluída e na `main`; aplicação sobe e os 2 testes passam.
- Tarefa 0 (issue #1) em andamento; próximo passo: tarefa 1 (`feature/tratamento-erros`).
