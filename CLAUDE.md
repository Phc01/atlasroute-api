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

- Pacote base `com.atlasroute.api`, organizado por domínio (ex.: `status/`, `auth/`, `vehicle/`)
- DTOs como `record`; controllers finos, regra no service
- Rotas versionadas: `/api/v1/...`
- Data/hora via bean `Clock` (testável); nunca `Instant.now()` sem clock
- Todo nome no código fica em inglês: pacotes, classes, métodos, variáveis, entidades,
  tabelas/colunas, campos do JSON e nomes de teste (ex.: `error/ResourceNotFoundException`,
  campo `errors`, `shouldReturnStatusUp`)
- Tabelas no plural em snake_case (`users`, `orders`, `packing_list_stops`); `user` e `order` são
  palavras reservadas no PostgreSQL
- Testes no padrão AAA com comentários `// Arrange`, `// Act`, `// Assert`
- Mensagens para o usuário, Javadoc, comentários e docs em português

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

| Entidade (tabela) | Conceito | Campos principais |
|---|---|---|
| `User` (`users`) | Usuário | name, email, password (hash), role (`ADMIN` / `DRIVER`) |
| `Vehicle` (`vehicles`) | Veículo | plate, weightCapacityKg, volumeCapacityM3 |
| `Driver` (`drivers`) | Motorista | name, licenseNumber (CNH), user |
| `Customer` (`customers`) | Cliente | name, address, latitude, longitude |
| `Order` (`orders`) | Pedido | customer, weightKg, volumeM3, status |
| `PackingList` (`packing_lists`) | Romaneio | vehicle, driver, date, status, totalDistanceKm |
| `PackingListStop` (`packing_list_stops`) | Parada do romaneio | packingList, order, sequence, distanceToNextKm |

## Tarefas da etapa 2 (uma branch cada)

| # | Branch | Escopo |
|---|---|---|
| 0 | `chore/readme-mvnw` | README com `.\mvnw.cmd` + nota do Testcontainers; `graphify-out/` no `.gitignore`; este CLAUDE.md |
| 1 | `feature/tratamento-erros` | `@RestControllerAdvice` com `ProblemDetail` (validação, 404, genérico) |
| 2 | `feature/user-entity` | Migration `V2` tabela `users`, entidade `User`, enum `Role`, repository |
| 3 | `feature/auth-register` | `POST /api/v1/auth/register`, BCrypt, validações, testes |
| 4 | `feature/auth-jwt-login` | Geração/validação de JWT + `POST /api/v1/auth/login` |
| 5 | `feature/security-config` | Rotas públicas (auth, Swagger, health), resto protegido, perfis, 401/403 em JSON, "Authorize" no Swagger |
| 6 | `test/auth-integration` | Fluxo completo com Testcontainers: cadastrar → logar → rota protegida |

## Estado atual

- Etapa 1 concluída e na `main`.
- Tarefa 0 (issue #1, PR #2) concluída.
- Tarefa 1 (PR #3) concluída: `error/GlobalExceptionHandler` + `ResourceNotFoundException`.
- Tarefa 2 (issue #4, PR #5) concluída: migration `V2__create_users.sql`, `user/User`, `Role`,
  `UserRepository`; testes de banco usam `TestcontainersConfiguration` (`@Import`).
- Tarefa 3 (issue #6, `feature/auth-register`) em andamento: `POST /api/v1/auth/register`
  (`auth/AuthController`, `RegistrationService`, `RegisterRequest`, `UserResponse`), BCrypt via
  `spring-security-crypto` (`config/PasswordConfig`; o starter de security só entra na tarefa 5),
  `error/ConflictException` → 409. 22 testes passando. Próximo passo: tarefa 4 (`feature/auth-jwt-login`).
- `@WebMvcTest` sempre com `controllers = ...`: sem filtro o slice carrega todos os controllers.
- `UserRepository.findByEmail`/`existsByEmail` normalizam o e-mail na consulta (`lower(trim())`),
  então o service passa o e-mail como foi digitado; o banco rejeita e-mail não normalizado
  (`ck_users_email_normalized`).

## Pendências apontadas na revisão (agente "Revisão de pull request")

- ✅ Tarefa 3: corrida no cadastro (violação de `uk_users_email` → 409) e normalização do e-mail
  (`@Email` no DTO + `User.normalizeEmail` no service) resolvidas no `RegistrationService`.
- Tarefa 4 (login): chamar `User.normalizeEmail` antes de `findByEmail`, como no cadastro.
- Tarefa 5: o `@ExceptionHandler(Exception.class)` do `GlobalExceptionHandler` captura
  `AccessDeniedException`/`AuthenticationException` de `@PreAuthorize` e devolveria 500;
  tratar essas exceções (403/401) antes do catch-all.
- Para 404 de recurso, os services lançam `ResourceNotFoundException("Veículo", id)`.

## Orquestração de agentes com o Orca ("bots")

O Claude Code pode atuar como **orquestrador**: outros agentes rodam no Orca, cada um no seu
worktree e terminal, e são controlados pelo **Orca CLI** (sempre com `--json`, lendo a saída
antes do próximo passo). Só usar quando o Pedro pedir para criar ou usar bots.

### Antes de começar
- Conferir o CLI: `orca status --json` (rodar num terminal aberto dentro do Orca)
- Listar worktrees existentes: `orca worktree ps --json` e pegar o `repoId` do atlasroute-api

### Criar um bot (um worktree por tarefa/issue)
1. `orca worktree create --repo id:<repoId> --name <nome-da-branch> --issue <numero> --json`
   (o nome segue os prefixos do fluxo: `feature/`, `fix/`, `chore/`, `test/`)
2. Iniciar o agente: `orca terminal create --worktree path:<caminho-do-worktree> --command "claude" --json`
3. Esperar ficar pronto: `orca terminal wait --for tui-idle --timeout-ms 30000 --json`

### Conversar com um bot
- Mandar a tarefa: `orca terminal send --text "<instrução completa>" --enter --json`
- Esperar terminar: `orca terminal wait --for tui-idle --timeout-ms 600000 --json`
- Ler a resposta: `orca terminal read --json`
- Ver os terminais/bots: `orca terminal list --json`
- Registrar status: `orca worktree set --worktree active --comment "<resumo>" --json`

### Regras para os bots
- Uma tarefa pequena por bot, em worktree separado, para não editarem os mesmos arquivos.
- Ao delegar, passar: objetivo, arquivos envolvidos, que siga as convenções deste CLAUDE.md e
  como testar (`.\mvnw.cmd test`; Docker Desktop aberto por causa do Testcontainers).
- Os bots **não** fazem commit, push nem PR: o Pedro roda os comandos git para praticar (ver
  "Fluxo de versionamento"). Ao terminar, o bot só sugere a mensagem de commit e o texto do PR.
- Depois que o bot terminar: revisar o diff (`orca file open-changed --mode both`) e rodar os
  testes antes de considerar a tarefa pronta.
- Se um bot travar ou errar duas vezes seguidas, parar e avisar o Pedro em vez de insistir.
- Nunca apagar worktrees (`orca worktree rm`) sem o Pedro pedir.
- No fim, resumir: qual bot fez o quê, resultado dos testes e o que ficou pendente.
