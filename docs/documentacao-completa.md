# GeriStreams — Documentação Técnica Completa

> **Propósito:** Sistema web fullstack para gerenciamento de assinaturas de streaming. O usuário cadastra suas assinaturas digitais, informa seu salário e recebe análise financeira em tempo real de quanto do orçamento está comprometido. Uma integração com IA (Claude / Anthropic) gera dicas personalizadas de economia. Administradores têm painel exclusivo para monitorar usuários e rankings globais.

---

## 1. Visão Geral da Arquitetura

```
GeriStreams/
├── backend/                          # Spring Boot 3.2.5 — porta 8080
│   └── src/main/java/com/projeto/
│       ├── controller/               # Endpoints REST (HTTP)
│       ├── service/                  # Lógica de negócio (BO)
│       ├── repository/               # Persistência (DAO)
│       ├── model/                    # Entidades JPA
│       ├── dto/                      # Data Transfer Objects
│       ├── security/                 # JWT + Spring Security
│       └── config/                   # Configurações globais
│   └── src/main/resources/
│       ├── application.properties    # Configurações (DB, JWT, Anthropic)
│       └── db/migration/             # Scripts Flyway (versionamento de schema)
│
└── frontend/                         # Angular 19+ — porta 4200
    └── src/app/
        ├── components/               # Telas (UI)
        ├── services/                 # Comunicação HTTP com API
        ├── models/                   # Interfaces TypeScript (DTOs)
        ├── guards/                   # Proteção de rotas
        └── interceptors/             # JWT automático em requisições
```

### Stack Tecnológica

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Backend | Spring Boot | 3.2.5 |
| Linguagem | Java | 21 |
| Build | Maven | 3.x |
| Frontend | Angular (Standalone) | 19+ |
| Linguagem | TypeScript | 5.x |
| UI | Bootstrap 5 + Bootstrap Icons | 5.3 |
| Banco de dados | PostgreSQL | 15+ |
| Migrations | Flyway | automático |
| Autenticação | JWT (JJWT 0.12.5) | — |
| IA | Anthropic Claude Haiku | claude-haiku-4-5 |
| PDF | OpenPDF | 1.3.30 |
| Docs API | SpringDoc OpenAPI (Swagger) | — |

---

## 2. Backend — Spring Boot

### 2.1 Modelos (Entidades JPA)

#### `Usuario.java`

```java
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;          // Chave de autenticação (único no banco)

    @Column(nullable = false)
    private String senha;          // Hash BCrypt, nunca texto puro

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal salario = BigDecimal.ZERO; // Base para cálculo de percentual

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER; // USER | ADMIN

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Assinatura> assinaturas = new ArrayList<>();
}
```

**Campos:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | Long | PK auto-incremento |
| `nome` | String | Nome do usuário (máx 100 chars) |
| `email` | String | Único no banco, usado como username |
| `senha` | String | Hash BCrypt |
| `salario` | BigDecimal | Base para cálculo financeiro |
| `role` | Role (enum) | USER (padrão) ou ADMIN |
| `createdAt` | LocalDateTime | Auditoria, não atualizável |
| `assinaturas` | List\<Assinatura\> | Relacionamento 1:N com cascade |

#### `Assinatura.java`

```java
@Entity
@Table(name = "assinaturas")
public class Assinatura {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;           // Ex: "Netflix", "Spotify"

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;      // Valor mensal (BigDecimal para precisão monetária)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategoriaAssinatura categoria; // Enum de categorias

    @Column(nullable = false)
    private Boolean ativo = true;  // Flag para pausar sem deletar

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;       // FK para o dono da assinatura

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

**Campos:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | Long | PK auto-incremento |
| `nome` | String | Nome do serviço |
| `valor` | BigDecimal | Custo mensal |
| `categoria` | CategoriaAssinatura (enum) | Classificação do serviço |
| `ativo` | Boolean | true = conta no resumo financeiro |
| `usuario` | Usuario | FK (LAZY: carregado somente quando necessário) |
| `createdAt` | LocalDateTime | Auditoria |

#### Enums

```java
public enum Role {
    USER,   // Usuário padrão
    ADMIN   // Administrador do sistema
}

public enum CategoriaAssinatura {
    STREAMING_VIDEO,   // Netflix, Disney+, HBO Max...
    STREAMING_MUSICA,  // Spotify, Apple Music...
    JOGOS,             // Xbox Game Pass, PS Plus...
    SOFTWARE,          // Adobe, Microsoft 365...
    NOTICIAS,          // Jornais digitais...
    OUTRO              // Categoria genérica
}
```

---

### 2.2 DTOs (Data Transfer Objects)

Os DTOs garantem que as entidades JPA **nunca sejam expostas diretamente** nos controllers.

| DTO | Direção | Campos principais |
|-----|---------|-------------------|
| `LoginRequestDTO` | Entrada | email, senha |
| `RegisterRequestDTO` | Entrada | nome, email, senha, salario |
| `JwtResponseDTO` | Saída | token, email, role |
| `AssinaturaRequestDTO` | Entrada | nome, valor, categoria |
| `AssinaturaResponseDTO` | Saída | id, nome, valor, categoria, ativo, createdAt |
| `UsuarioResponseDTO` | Saída | id, nome, email, salario, role, createdAt (sem senha!) |
| `AtualizarSalarioDTO` | Entrada | salario |
| `ResumoFinanceiroDTO` | Saída | salario, totalMensal, percentualDoSalario, assinaturas, gastosPorCategoria |
| `RankingAssinaturaDTO` | Saída | nome, quantidadeUsuarios, gastoTotal, gastoMedio |
| `AiDicasResponseDTO` | Saída | dicas (texto Markdown gerado pela IA) |

---

### 2.3 Controllers (Endpoints REST)

#### `AuthController` — `/api/auth` — Público

| Método | Rota | Acesso | Status | Descrição |
|--------|------|--------|--------|-----------|
| POST | `/register` | Público | 201 Created | Cadastra novo usuário e retorna JWT |
| POST | `/login` | Público | 200 OK | Autentica e retorna JWT |

**Exemplo de uso — Login:**
```json
// POST /api/auth/login
// Body:
{ "email": "joao@email.com", "senha": "123456" }

// Response 200:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "joao@email.com",
  "role": "USER"
}
```

#### `AssinaturaController` — `/api/subscriptions` — Requer JWT

| Método | Rota | Status | Descrição |
|--------|------|--------|-----------|
| GET | `/` | 200 | Lista assinaturas do usuário autenticado |
| POST | `/` | 201 | Cria nova assinatura |
| PUT | `/{id}` | 200 | Atualiza assinatura (valida propriedade) |
| DELETE | `/{id}` | 204 | Remove assinatura (valida propriedade) |
| PATCH | `/{id}/toggle` | 200 | Alterna ativo/inativo |
| GET | `/resumo` | 200 | Retorna resumo financeiro completo |

**Exemplo — Resumo financeiro:**
```json
// GET /api/subscriptions/resumo
// Response 200:
{
  "salario": 5000.00,
  "totalMensal": 385.50,
  "percentualDoSalario": 7.71,
  "assinaturas": [
    { "id": 1, "nome": "Netflix", "valor": 49.90, "categoria": "STREAMING_VIDEO", "ativo": true }
  ],
  "gastosPorCategoria": {
    "STREAMING_VIDEO": 250.00,
    "STREAMING_MUSICA": 80.00,
    "SOFTWARE": 55.50
  }
}
```

#### `UsuarioController` — `/api/users` — Requer JWT

| Método | Rota | Status | Descrição |
|--------|------|--------|-----------|
| GET | `/me` | 200 | Dados do usuário autenticado (sem senha) |
| PUT | `/me/salario` | 200 | Atualiza salário |

#### `AdminController` — `/api/admin` — Requer ADMIN

| Método | Rota | Status | Descrição |
|--------|------|--------|-----------|
| GET | `/users` | 200 | Lista todos os usuários |
| GET | `/users/{id}` | 200 | Dados de usuário específico |
| GET | `/users/{id}/subscriptions` | 200 | Assinaturas de qualquer usuário |
| PATCH | `/users/{id}/promote` | 200 | Promove usuário para ADMIN |
| GET | `/ranking` | 200 | Ranking global de serviços |

#### `RelatorioController` — `/api/reports` — Requer JWT

| Método | Rota | Status | Descrição |
|--------|------|--------|-----------|
| GET | `/pdf` | 200 | Exporta relatório financeiro em PDF |

#### `AiController` — `/api/ai` — Requer JWT

| Método | Rota | Status | Descrição |
|--------|------|--------|-----------|
| GET | `/dicas` | 200 | Gera dicas personalizadas via Claude AI |

---

### 2.4 Camada de Service (Lógica de Negócio)

**Princípio:** Toda regra de negócio vive no Service. Controllers apenas delegam.

#### `AuthService`

| Método | Parâmetros | Retorno | Descrição |
|--------|-----------|---------|-----------|
| `registrar(dto)` | RegisterRequestDTO | JwtResponseDTO | Valida unicidade de email, hash BCrypt, salva e gera token |
| `login(dto)` | LoginRequestDTO | JwtResponseDTO | Valida credenciais via AuthenticationManager, gera token |

**Fluxo de registro:**
```
RegisterRequestDTO → verificar email duplicado → encode senha (BCrypt)
→ salvar Usuario → loadUserByUsername → generateToken → JwtResponseDTO
```

#### `UsuarioService`

| Método | Parâmetros | Retorno | Descrição |
|--------|-----------|---------|-----------|
| `buscarPerfil()` | — | UsuarioResponseDTO | Perfil do usuário autenticado |
| `atualizarSalario(dto)` | AtualizarSalarioDTO | UsuarioResponseDTO | Atualiza salário com @Transactional |
| `listarTodos()` | — | List\<UsuarioResponseDTO\> | Operação administrativa |
| `buscarPorId(id)` | Long | UsuarioResponseDTO | Busca ou lança exceção |
| `promoverParaAdmin(id)` | Long | UsuarioResponseDTO | Valida auto-promoção e dupla promoção |
| `getUsuarioAutenticado()` | — | Usuario (Entity) | Método auxiliar — lê do SecurityContextHolder |

#### `AssinaturaService`

| Método | Parâmetros | Retorno | Descrição |
|--------|-----------|---------|-----------|
| `listar()` | — | List\<AssinaturaResponseDTO\> | Assinaturas do usuário autenticado |
| `criar(dto)` | AssinaturaRequestDTO | AssinaturaResponseDTO | Associa ao usuário e salva |
| `atualizar(id, dto)` | Long, AssinaturaRequestDTO | AssinaturaResponseDTO | Valida propriedade antes de atualizar |
| `remover(id)` | Long | void | Valida propriedade antes de deletar |
| `alternarAtivo(id)` | Long | AssinaturaResponseDTO | Toggle `!ativo` |
| `calcularResumoFinanceiro()` | — | ResumoFinanceiroDTO | SUM, percentual, GROUP BY categoria |
| `rankingServicos()` | — | List\<RankingAssinaturaDTO\> | Query agregada global |
| `listarPorUsuario(usuarioId)` | Long | List\<AssinaturaResponseDTO\> | Visão administrativa |
| `buscarPorIdDoUsuario(id)` | Long | Assinatura (privado) | Valida que assinatura pertence ao usuário |

**Lógica de percentual:**
```java
BigDecimal percentual = salario.compareTo(BigDecimal.ZERO) > 0
    ? total.divide(salario, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
    : BigDecimal.ZERO;
```

#### `RelatorioService`

Gera PDF com a biblioteca **OpenPDF** inteiramente em memória (`ByteArrayOutputStream`), sem gravar em disco. O PDF contém:
- Cabeçalho com nome, email e data
- Tabela de resumo financeiro
- Tabela de assinaturas ativas
- Tabela de gastos por categoria
- Rodapé

#### `AiService`

Constrói um prompt personalizado com os dados financeiros do usuário e chama a **API Anthropic (Claude)** via `RestClient`.

**Fluxo:**
```
calcularResumoFinanceiro() → construirPrompt(nome, resumo)
→ POST https://api.anthropic.com/v1/messages → extrair texto → AiDicasResponseDTO
```

**Prompt template:**
```
"Olá! Me chamo {nome}.
Salário: R$ {salario} | Gasto total: R$ {total} | {percentual}% comprometido
Assinaturas ativas: Netflix (STREAMING_VIDEO) — R$ 49.90/mês, ...
Gastos por categoria: STREAMING_VIDEO: R$ 250, ...
Me dê 1 dica prática e personalizada..."
```

---

### 2.5 Camada de Repository (Persistência)

#### `UsuarioRepository extends JpaRepository<Usuario, Long>`

| Método | Query Gerada | Retorno |
|--------|-------------|---------|
| `findByEmail(email)` | `SELECT * FROM usuarios WHERE email = ?` | `Optional<Usuario>` |
| `existsByEmail(email)` | `SELECT COUNT(*) > 0 FROM usuarios WHERE email = ?` | `boolean` |
| + todos herdados de JpaRepository | `findAll`, `findById`, `save`, `delete`... | variado |

#### `AssinaturaRepository extends JpaRepository<Assinatura, Long>`

| Método | Query | Retorno |
|--------|-------|---------|
| `findByUsuarioId(id)` | `WHERE usuario_id = ?` | `List<Assinatura>` |
| `findByUsuarioIdAndAtivoTrue(id)` | `WHERE usuario_id = ? AND ativo = true` | `List<Assinatura>` |
| `sumValorAtivoByUsuarioId(id)` | `SELECT SUM(a.valor) WHERE usuario_id = ? AND ativo = true` | `BigDecimal` (null se vazio) |
| `sumValorGroupedByCategoriaAndUsuarioId(id)` | `SELECT a.categoria, SUM(a.valor) ... GROUP BY a.categoria` | `List<Object[]>` |
| `rankingGastosPorUsuario()` | `SELECT a.usuario.id, SUM(a.valor) ... GROUP BY a.usuario.id ORDER BY SUM DESC` | `List<Object[]>` |
| `rankingServicos()` | `SELECT a.nome, COUNT(a), SUM(a.valor), AVG(a.valor) ... GROUP BY a.nome ORDER BY SUM DESC` | `List<Object[]>` |

---

### 2.6 Segurança (JWT + Spring Security)

#### Fluxo completo de autenticação:

```
1. POST /api/auth/login { email, senha }
2. AuthService → AuthenticationManager.authenticate()
3. BCrypt.matches(senha, hashNoBanco) ✓
4. JwtUtil.generateToken(userDetails)
5. Resposta: { token: "eyJ..." }

--- Requisições futuras ---

6. Frontend envia: Authorization: Bearer eyJ...
7. JwtAuthFilter intercepta TODA requisição
8. jwtUtil.extractEmail(token) → email
9. userDetailsService.loadUserByUsername(email)
10. jwtUtil.isTokenValid(token, userDetails) ✓
11. SecurityContextHolder.setAuthentication(authToken)
12. Controller executa normalmente
```

#### `JwtUtil`

| Método | Descrição |
|--------|-----------|
| `generateToken(userDetails)` | Cria JWT com subject=email, claim roles, expiração de 24h, assinado com HMAC-SHA256 |
| `extractEmail(token)` | Faz parse e retorna o subject (email) |
| `isTokenValid(token, userDetails)` | Valida email + não expirado |
| `isTokenExpired(token)` | Compara `expiration.before(new Date())` |
| `parseClaims(token)` | Parse seguro com validação de assinatura (privado) |

#### `JwtAuthFilter extends OncePerRequestFilter`

Executa uma vez por requisição:
1. Lê header `Authorization: Bearer <token>`
2. Extrai e valida o token
3. Se válido: coloca `Authentication` no `SecurityContextHolder`
4. Passa para o próximo filtro

#### `SecurityConfig`

```java
http
  .cors(withDefaults())                          // Permite localhost:4200
  .csrf(disable)                                 // Desnecessário com JWT stateless
  .sessionManagement(STATELESS)                  // Sem sessão no servidor
  .requestMatchers("/api/auth/**").permitAll()   // Login/registro: público
  .requestMatchers("/api/admin/**").hasRole("ADMIN") // Admin: somente ADMIN
  .anyRequest().authenticated()                  // Resto: precisa de JWT
  .addFilterBefore(jwtAuthFilter, ...)           // JWT antes do filtro padrão
```

#### `GlobalExceptionHandler`

| Exceção | Status HTTP | Uso |
|---------|------------|-----|
| `MethodArgumentNotValidException` | 400 | Validação de DTO (@Valid) |
| `IllegalArgumentException` | 400 | Regras de negócio (email duplicado, não encontrado) |
| `BadCredentialsException` | 401 | Email ou senha incorretos |
| `AccessDeniedException` | 403 | Usuário sem a role necessária |
| `Exception` (genérico) | 500 | Erros inesperados (bug) |

**Formato padrão de resposta de erro:**
```json
{ "error": "Mensagem descritiva", "code": 400 }
```

---

### 2.7 Configurações (`application.properties`)

```properties
server.port=8080

# PostgreSQL (variáveis de ambiente com fallback)
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/geristreams}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:admin}

# Flyway — migrações versionadas automáticas
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# JWT — 24h de expiração
app.jwt.secret=${JWT_SECRET:geristreams-secret-key-...}
app.jwt.expiration-ms=86400000

# Swagger UI em /swagger-ui.html
springdoc.swagger-ui.path=/swagger-ui.html

# Anthropic Claude Haiku
anthropic.api.key=${ANTHROPIC_API_KEY:}
anthropic.model=claude-haiku-4-5

# CORS: permite origem do Angular
app.cors.allowed-origins=http://localhost:4200
```

---

## 3. Frontend — Angular 19+

### 3.1 Padrões Adotados

- **Standalone Components** — sem NgModules
- **Signals** — gestão de estado reativa (`signal()`, `computed()`) no lugar de BehaviorSubject
- **Built-in Control Flow** — `@if`, `@for`, `@switch` (sem `*ngIf`, `*ngFor` — depreciados)
- **Functional Guards e Interceptors** — sem classes `implements CanActivate`
- **Bootstrap 5** — responsividade e UI profissional

---

### 3.2 Services Angular

#### `AuthService`

```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _loggedIn = signal(this.hasToken()); // Estado reativo
  readonly loggedIn = this._loggedIn.asReadonly();       // Exposto como somente leitura

  login(payload): Observable<JwtResponse>    // POST /api/auth/login + storeSession()
  register(payload): Observable<JwtResponse> // POST /api/auth/register + storeSession()
  logout(): void                             // Remove localStorage, seta signal false, navega /login
  getToken(): string | null                  // JWT do localStorage
  getRole(): string | null                   // 'USER' | 'ADMIN'
  isAdmin(): boolean                         // getRole() === 'ADMIN'
}
```

| Método | Parâmetros | Retorno | Descrição |
|--------|-----------|---------|-----------|
| `login` | `LoginRequest` | `Observable<JwtResponse>` | Chama backend e persiste sessão |
| `register` | `RegisterRequest` | `Observable<JwtResponse>` | Registra e já loga |
| `logout` | — | `void` | Remove token, atualiza signal, redireciona |
| `getToken` | — | `string \| null` | Token do localStorage |
| `isAdmin` | — | `boolean` | Verifica role ADMIN |

#### `AssinaturaService`

```typescript
listar(): Observable<AssinaturaResponse[]>
criar(payload): Observable<AssinaturaResponse>     // POST body: { nome, valor, categoria }
atualizar(id, payload): Observable<AssinaturaResponse> // PUT /{id}
remover(id): Observable<void>                      // DELETE /{id}
toggleAtivo(id): Observable<AssinaturaResponse>   // PATCH /{id}/toggle
resumoFinanceiro(): Observable<ResumoFinanceiro>  // GET /resumo
```

#### `AiService`

```typescript
gerarDicas(): Observable<AiDicasResponse>  // GET /api/ai/dicas → { dicas: string (Markdown) }
```

---

### 3.3 Interceptadores e Guards

#### `jwtInterceptor` (Functional Interceptor)

Adiciona automaticamente o header `Authorization: Bearer <token>` em **toda** requisição HTTP saindo do Angular.

```typescript
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).getToken();
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
```

#### `authGuard` (Functional Guard)

Protege rotas que exigem autenticação. Se não há token, redireciona para `/login`.

```typescript
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.getToken() ? true : router.createUrlTree(['/login']);
};
```

#### `adminGuard`

Idêntico ao `authGuard`, mas verifica `isAdmin()`. Protege a rota `/admin`.

#### `errorInterceptor`

Intercepta respostas de erro HTTP. Redireciona para `/login` automaticamente em caso de status 401.

---

### 3.4 Componentes

| Componente | Rota | Descrição |
|-----------|------|-----------|
| `HomeComponent` | `/` | Landing page |
| `LoginComponent` | `/login` | Formulário de login |
| `RegisterComponent` | `/register` | Formulário de registro |
| `DashboardComponent` | `/dashboard` | Resumo financeiro, gráficos, barra de progresso |
| `SubscriptionsComponent` | `/subscriptions` | CRUD de assinaturas |
| `AiTipsComponent` | `/ai-tips` | Dicas geradas por IA |
| `AdminComponent` | `/admin` | Painel administrativo (lista usuários, ranking) |
| `NavbarComponent` | — | Barra de navegação com estado reativo |

---

### 3.5 Modelos TypeScript (Interfaces)

```typescript
// auth.model.ts
interface LoginRequest   { email: string; senha: string; }
interface RegisterRequest { nome: string; email: string; senha: string; salario: number; }
interface JwtResponse     { token: string; email: string; role: string; }

// assinatura.model.ts
interface AssinaturaRequest  { nome: string; valor: number; categoria: string; }
interface AssinaturaResponse { id: number; nome: string; valor: number; categoria: string; ativo: boolean; createdAt: string; }

// financeiro.model.ts
interface ResumoFinanceiro {
  salario: number;
  totalMensal: number;
  percentualDoSalario: number;
  assinaturas: AssinaturaResponse[];
  gastosPorCategoria: { [categoria: string]: number };
}

// usuario.model.ts
interface UsuarioResponse { id: number; nome: string; email: string; salario: number; role: string; createdAt: string; }
```

---

## 4. Fluxos Principais

### Fluxo de Login Completo

```
[Angular LoginComponent]
    ↓ submit
AuthService.login({ email, senha })
    ↓ POST /api/auth/login
[Spring AuthController]
    ↓ chama
AuthService.login(dto)
    ↓ AuthenticationManager.authenticate(email, senha)
    ↓ BCrypt.matches(senha, hash) ✓
    ↓ JwtUtil.generateToken(userDetails)
    ↓ retorna JwtResponseDTO
[Angular]
    ↓ localStorage.setItem(token)
    ↓ _loggedIn.set(true) → signal atualiza NavBar automaticamente
    ↓ navigate('/dashboard')
```

### Fluxo de Assinatura com Segurança

```
[Angular SubscriptionsComponent]
    ↓ DELETE /api/subscriptions/5
jwtInterceptor → adiciona header Authorization: Bearer eyJ...
    ↓
[Spring JwtAuthFilter]
    ↓ extrai email do token
    ↓ SecurityContextHolder.setAuthentication(...)
[AssinaturaController.remover(5)]
    ↓ assinaturaService.remover(5)
[AssinaturaService.buscarPorIdDoUsuario(5)]
    ↓ assinaturaRepository.findById(5)
    ↓ filter: assinatura.getUsuario().getId().equals(usuario.getId())
    ↓ Se não pertence: IllegalArgumentException → 400
    ↓ Se pertence: assinaturaRepository.delete(assinatura) → 204
```

### Fluxo de Dicas de IA

```
[Angular AiTipsComponent]
    ↓ GET /api/ai/dicas
[Spring AiController]
    ↓ aiService.gerarDicas()
AssinaturaService.calcularResumoFinanceiro() → dados do usuário
UsuarioService.getUsuarioAutenticado() → nome
construirPrompt(nome, resumo) → texto personalizado
RestClient.post("/v1/messages").body(anthropicRequest)
    ↓ Claude Haiku processa
AnthropicResponseDTO → extractText()
    ↓ { "dicas": "# Análise...\n\n**Recomendação:**..." }
[Angular]
    ↓ marked.parse(res.dicas) → HTML
    ↓ [innerHTML] exibe no template
```

---

## 5. Sugestão Extra

**Ponto de melhoria:** O método `construirPrompt()` no `AiService` usa `StringBuilder` de forma manual. Uma alternativa mais elegante seria usar **Java Text Blocks** (triple-quoted strings) com `formatted()` ou `String.format()`, tornando o template do prompt mais legível e fácil de manter:

```java
// Antes (atual): StringBuilder com múltiplos .append()
// Depois (melhoria):
private String construirPrompt(String nome, ResumoFinanceiroDTO resumo) {
    return """
        Olá! Me chamo %s.
        Salário mensal: R$ %s | Gasto total: R$ %s | %s%% comprometido
        ...
        """.formatted(nome, formatar(resumo.salario()), formatar(resumo.totalMensal()), ...);
}
```

**Outro ponto:** `rankingGastosPorUsuario()` no `AssinaturaRepository` é declarado mas não usado em nenhum Service/Controller. Pode ser removido para reduzir superfície de código não testado.