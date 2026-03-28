# Contexto do Projeto: GeriStreams — Gerenciador de Assinaturas Digitais

## 1. Domínio e Visão Geral
**GeriStreams** é um sistema de gestão de assinaturas digitais (streaming, software, jogos, notícias) com análise financeira. O usuário cadastra suas assinaturas (Netflix, Spotify, etc.), e o sistema calcula o impacto no salário, categoriza os gastos, gera relatórios PDF e oferece dicas de economia via IA (Claude Haiku da Anthropic).

**Projeto acadêmico** — IFG Campus Luziânia, disciplina Programação Web, Prof. Daniel Lucena.

## 2. Stack Tecnológica
- **Frontend:** Angular 21 com Signals e TypeScript (standalone components)
- **UI/Styling:** Bootstrap 5 + Bootstrap Icons
- **Backend:** Java 21 com Spring Boot 3 (Web, Data JPA, Security, Validation, Actuator)
- **Banco de Dados:** PostgreSQL 16 via Flyway migrations
- **Autenticação:** JWT com Spring Security (roles: USER, ADMIN)
- **IA:** Anthropic Claude Haiku (`claude-haiku-4-5`) para dicas de economia
- **PDF:** OpenPDF para geração de relatórios financeiros
- **Documentação API:** SpringDoc OpenAPI 2 (Swagger UI em `/swagger-ui.html`)

## 3. Arquitetura e Padrões (OBRIGATÓRIO respeitar)

### Backend — Separação de Camadas
```
Controller → Service → Repository → Entity (banco)
     ↓           ↓
    DTO         DTO
```
- **Entity** (`model/`): Mapeamento JPA. NUNCA exposta fora do Service.
- **Repository** (`repository/`): Interface Spring Data JPA. Apenas persistência.
- **Service** (`service/`): TODA a lógica de negócio, validações e ownership checks. `@Transactional` em mutations.
- **DTO** (`dto/`): Request DTOs são Java records com Bean Validation. Response DTOs têm `fromEntity()`. **NUNCA expor Entity nos Controllers.**
- **Controller** (`controller/`): Endpoints REST. `@Valid` em todos os `@RequestBody`. Retorna sempre DTO.

### Frontend — Angular 21
- Componentes: `standalone: true`, estado via `signal()`, derivações via `computed()`
- Templates: `@if`, `@for`, `@switch` — **PROIBIDO** `*ngIf`, `*ngFor`
- Injeção: `inject()` — preferido sobre constructor injection em componentes
- Rotas: `loadComponent` (lazy loading) para todos os componentes

## 4. Estrutura de Pastas

### Backend
```
backend/src/main/java/com/projeto/
├── config/          # SecurityConfig, CorsConfig, OpenApiConfig, RestClientConfig
├── controller/      # REST endpoints (AuthController, AssinaturaController, etc.)
├── service/         # Lógica de negócio (AuthService, AssinaturaService, etc.)
├── repository/      # Spring Data JPA (UsuarioRepository, AssinaturaRepository)
├── dto/             # DTOs organizados por domínio (auth/, assinatura/, usuario/, ai/, etc.)
├── model/           # JPA Entities (Usuario, Assinatura, enums)
└── security/        # JWT (JwtUtil, JwtAuthFilter, UserDetailsServiceImpl)

backend/src/main/resources/
├── application.properties
└── db/migration/    # Flyway: V1__create_tables.sql, V2__..., etc.
```

### Frontend
```
frontend/src/app/
├── components/      # UI (dashboard/, subscriptions/, admin/, ai-tips/, home/, login/, register/, navbar/)
├── services/        # HTTP (auth.service.ts, assinatura.service.ts, usuario.service.ts, ai.service.ts, relatorio.service.ts)
├── models/          # Interfaces TypeScript (auth.model.ts, assinatura.model.ts, usuario.model.ts, financeiro.model.ts)
├── guards/          # authGuard, adminGuard (funcionais: CanActivateFn)
├── interceptors/    # jwt.interceptor.ts, error.interceptor.ts (HttpInterceptorFn)
└── environments/    # environment.ts, environment.prod.ts
```

## 5. Fluxo de Desenvolvimento

**Ao criar/modificar qualquer funcionalidade que altere o schema:**
1. Verificar o número da última migration: `backend/src/main/resources/db/migration/V*.sql`
2. Criar a migration `VN__descricao.sql` ANTES de qualquer código Java
3. Criar/modificar a Entity JPA com anotações consistentes com o SQL
4. Criar Repository → DTOs → Service → Controller
5. Criar Model TypeScript → Service Angular → Component → Rota

## 6. Regras para a IA

### Obrigatório
- `PascalCase` para classes Java/TypeScript, `camelCase` para métodos/variáveis
- Logger Slf4j em todo Service e Controller (`LoggerFactory.getLogger(Classe.class)`)
- `@Transactional` em todos os métodos de Service que mutam dados
- `@Valid` em todos os `@RequestBody` nos Controllers
- `@Operation`, `@Tag`, `@SecurityRequirement(name = "bearerAuth")` em endpoints protegidos
- `signal()`, `computed()` para estado no Angular
- `@if`/`@for`/`@switch` nos templates Angular
- `standalone: true` em todos os componentes Angular
- Gerar migration Flyway para toda alteração de schema

### Proibido
- Expor Entity JPA no Controller (sempre usar DTO)
- `*ngIf`, `*ngFor`, `*ngSwitch` nos templates Angular
- `System.out.println` ou `console.log` em código submetido
- `@Autowired` em campo (usar constructor injection)
- Hardcodar secrets ou API keys no código
- Try/catch vazios ou que engolem exceções silenciosamente

## 7. Variáveis de Ambiente
```properties
# Backend (application.properties lê do .env via spring-dotenv)
DB_URL=jdbc:postgresql://localhost:5432/geristreams
DB_USERNAME=postgres
DB_PASSWORD=admin
JWT_SECRET=geristreams-secret-key-256-bits
ANTHROPIC_API_KEY=sk-ant-...

# Frontend (environment.ts)
apiUrl=http://localhost:8080
```

## 8. Executar o Projeto (desenvolvimento local)
```bash
# Backend (porta 8080)
cd backend && ./mvnw spring-boot:run

# Frontend (porta 4200)
cd frontend && npm start

# Banco de dados (Docker)
docker compose up db -d
```

Swagger UI disponível em: `http://localhost:8080/swagger-ui.html`
