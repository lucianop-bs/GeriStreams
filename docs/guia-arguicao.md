# Guia de Arguição — GeriStreams

> Documento de preparação para defesa técnica oral.
> Organizado por temas do mais provável ao mais específico.

---

## PITCH DO PROJETO (abra com isso)

**O que é o GeriStreams?**

> "O GeriStreams é um sistema web fullstack de gerenciamento financeiro pessoal
> focado em assinaturas digitais — streaming, software, serviços. O usuário cadastra
> suas assinaturas, o sistema calcula quanto ele gasta por mês, qual percentual do
> salário isso representa, e ainda gera dicas financeiras personalizadas usando
> Inteligência Artificial. Temos também uma área administrativa para controle de todos
> os usuários e um relatório exportável em PDF."

**Stack em uma linha:**
> "Angular 19 no frontend com TypeScript e Bootstrap 5, Spring Boot 3 no backend com
> Java 21, PostgreSQL como banco, autenticação via JWT e integração com a API da Anthropic
> para geração de dicas com IA."

---

## 1. ARQUITETURA — PERGUNTAS PROVÁVEIS

### "Por que você separou em camadas?"

A separação de responsabilidades (princípio S do SOLID) garante que cada parte do código
faça apenas uma coisa. Isso facilita manutenção, testes e evolução do sistema.

```
Controller  →  recebe a requisição HTTP, valida, chama o Service
Service     →  executa a regra de negócio, chama o Repository
Repository  →  conversa com o banco de dados
DTO         →  trafega dados entre camadas sem expor a Entity
Entity      →  representa a tabela no banco (mapeamento JPA)
```

### "O que é DTO e por que usar?"

DTO = Data Transfer Object. É um objeto criado só para trafegar dados.

**Por que não usar a Entity diretamente no Controller?**
- A Entity tem anotações JPA (`@ManyToOne`, `@OneToMany`, lazy loading) — expor isso pode
  causar serialização infinita ou `LazyInitializationException`
- O DTO controla exatamente o que o frontend recebe — sem expor dados sensíveis como a senha
- Segurança: com DTO de entrada (`AssinaturaRequestDTO`), o frontend não pode definir campos
  como `usuarioId` ou `ativo` — esses são definidos pelo servidor

**Exemplo real no projeto:**
```java
// O frontend envia apenas:
{ "nome": "Netflix", "valor": 39.90, "categoria": "STREAMING_VIDEO" }

// O servidor define quem é o dono (usuário autenticado) — o frontend não manda isso
assinatura.setUsuario(usuarioService.getUsuarioAutenticado());
```

### "O que é o padrão fromEntity()?"

É o padrão Factory Method aplicado ao DTO. Em vez de converter em vários lugares,
a lógica de conversão fica dentro do próprio DTO como método estático.

```java
// AssinaturaResponseDTO.java
public static AssinaturaResponseDTO fromEntity(Assinatura a) {
    return new AssinaturaResponseDTO(
        a.getId(), a.getNome(), a.getValor(), a.getCategoria().name(), a.getAtivo()
    );
}

// Uso no Service — sempre via método estático:
.map(AssinaturaResponseDTO::fromEntity)  // Method Reference
```

**Por que é Factory Method?**
Porque a criação do objeto está encapsulada em um método estático nomeado — você não chama
`new AssinaturaResponseDTO(...)` espalhado pelo código, chama `fromEntity()`.

---

## 2. SEGURANÇA — JWT (mais provável de cair)

### "Como funciona a autenticação no seu sistema?"

Fluxo completo de **registro** e **login**:

```
[1] Frontend envia POST /api/auth/register ou /api/auth/login
[2] AuthController recebe e chama AuthService
[3] AuthService verifica email/senha
[4] JwtUtil.generateToken() cria o token JWT
[5] Frontend recebe o token e armazena (localStorage)
[6] Em toda requisição futura: header "Authorization: Bearer <token>"
[7] JwtAuthFilter intercepta ANTES do Controller
[8] JwtAuthFilter valida o token e autentica o usuário no SecurityContext
[9] Controller executa normalmente
```

### "O que é JWT e como ele funciona?"

JWT = JSON Web Token. É uma string em 3 partes separadas por ponto:

```
eyJhbGciOiJIUzI1NiJ9  .  eyJzdWIiOiJqb2FvQGVtYWlsLmNvbSJ9  .  assinatura
     HEADER                        PAYLOAD                       SIGNATURE
  (algoritmo)               (dados: email, roles, expiração)    (garante integridade)
```

- **Header**: algoritmo usado (HMAC-SHA256)
- **Payload**: informações do usuário — email (subject), roles, validade
- **Signature**: hash do header + payload usando a chave secreta — impede falsificação

**Stateless**: o servidor não guarda sessão. Tudo está no token. Isso permite escalar
o sistema horizontalmente (vários servidores) sem sincronizar sessões.

### "Por que BCrypt na senha?"

BCrypt é um algoritmo de hash **unidirecional e lento por design**.

- `passwordEncoder.encode("123456")` → `"$2a$10$xyz..."` (hash diferente a cada chamada)
- `passwordEncoder.matches("123456", hash)` → compara sem revelar a senha original
- **Por que lento?** Para dificultar ataques de força bruta — mesmo com a senha `123456`,
  calcular milhões de tentativas leva muito tempo

### "O que é o JwtAuthFilter?"

É um filtro Spring que executa em **toda requisição**, antes de chegar ao Controller.

```
Requisição HTTP chega
        ↓
JwtAuthFilter.doFilterInternal()
        ↓
[1] Extrai o header "Authorization: Bearer <token>"
[2] Se não tem token → passa para o próximo filtro (será rejeitado depois pelo Security)
[3] Extrai o email do token (JwtUtil.extractEmail)
[4] Carrega o usuário do banco pelo email
[5] Valida o token (assinatura + expiração + email)
[6] Se válido → coloca no SecurityContextHolder (usuário autenticado para esta thread)
[7] Continua a requisição → chega ao Controller
```

### "O que é SecurityContextHolder?"

É o mecanismo do Spring para guardar, durante uma requisição, quem é o usuário autenticado.
Usa **ThreadLocal** internamente — cada thread (requisição) tem seu próprio contexto.

```java
// Como o UsuarioService acessa o usuário autenticado:
SecurityContextHolder.getContext().getAuthentication().getName()
// Retorna o email do usuário — definido pelo JwtAuthFilter no começo da requisição
```

### "O que é CSRF e por que vocês desabilitaram?"

CSRF = Cross-Site Request Forgery. É um ataque onde um site malicioso força o navegador
do usuário a fazer requisições não autorizadas.

**Por que desabilitamos aqui?** Porque usamos JWT no header `Authorization`.
CSRF só funciona quando a autenticação é baseada em cookies. Com JWT no header,
o ataque não funciona — o site malicioso não tem acesso ao token.

---

## 3. JPA / BANCO DE DADOS

### "O que é JPA e como usam no projeto?"

JPA = Java Persistence API. É uma especificação que mapeia classes Java para tabelas SQL.
O Hibernate é a implementação usada (configurado automaticamente pelo Spring Boot).

```java
@Entity
@Table(name = "assinaturas")
public class Assinatura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // auto_increment
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)   // muitas assinaturas → um usuário
    @JoinColumn(name = "usuario_id")     // coluna FK na tabela
    private Usuario usuario;
}
```

### "O que é FetchType.LAZY?"

**LAZY** = o relacionamento só é carregado do banco quando você chamar `assinatura.getUsuario()`.
**EAGER** = carrega junto com a entidade principal sempre.

Usamos LAZY porque nem sempre precisamos do usuário quando buscamos uma assinatura.
Carregamento sob demanda = performance melhor.

**Atenção: LazyInitializationException** — se você tentar acessar um relacionamento LAZY
fora de uma sessão JPA aberta (fora de uma transação), o Hibernate lança essa exceção.
É por isso que usamos `@Transactional` nos Services e convertemos para DTO ainda dentro
da transação.

### "O que é @Transactional?"

Garante que um conjunto de operações no banco seja executado de forma **atômica** —
tudo funciona, ou nada funciona (rollback automático em caso de exceção).

```java
@Transactional
public AssinaturaResponseDTO criar(AssinaturaRequestDTO dto) {
    // Se qualquer linha aqui lançar exceção →
    // o Spring faz ROLLBACK automaticamente
    assinatura.setNome(dto.nome());
    assinaturaRepository.save(assinatura);  // INSERT
    // Se tudo OK → COMMIT automático ao final do método
}
```

**Como funciona por baixo?** Spring AOP cria um **Proxy** da classe Service.
Quando você chama `assinaturaService.criar()`, na verdade está chamando o proxy,
que abre a transação antes e faz commit/rollback depois.

### "Por que BigDecimal para valores monetários?"

`double` e `float` têm imprecisão de representação binária:
```java
// PROBLEMA com double:
0.1 + 0.2 = 0.30000000000000004  // ERRADO! Nunca use para dinheiro

// CORRETO com BigDecimal:
BigDecimal.valueOf(0.1).add(BigDecimal.valueOf(0.2)) = 0.3  // Certo!
```

**Operações no projeto:**
```java
// Divisão com escala e arredondamento explícito:
total.divide(salario, 4, RoundingMode.HALF_UP)
// 4 = casas decimais no resultado
// HALF_UP = 0.555 → 0.56 (arredonda metade para cima — padrão financeiro)

// Multiplicar por 100 para percentual:
.multiply(BigDecimal.valueOf(100))
```

### "O que são os @Query no Repository?"

Spring Data JPA oferece duas formas de criar queries:

**1. Query Methods (automático por nome):**
```java
// Spring Data lê o nome e gera o SQL automaticamente:
findByUsuarioId(Long id)
// SELECT * FROM assinaturas WHERE usuario_id = ?

findByUsuarioIdAndAtivoTrue(Long id)
// SELECT * FROM assinaturas WHERE usuario_id = ? AND ativo = true
```

**2. @Query com JPQL (manual):**
```java
@Query("SELECT SUM(a.valor) FROM Assinatura a WHERE a.usuario.id = :usuarioId AND a.ativo = true")
BigDecimal sumValorAtivoByUsuarioId(@Param("usuarioId") Long usuarioId);
```

JPQL usa nomes de **classes e atributos Java**, não de tabelas SQL.
Hibernate converte para SQL na hora.

---

## 4. JAVA — CONCEITOS TÉCNICOS

### "Explica o uso de Streams no projeto"

Streams são uma forma funcional de processar coleções de dados em Java.
A lógica fica encadeada (pipeline), sem loops manuais.

```java
// Exemplo do AssinaturaService.listar():
assinaturaRepository.findByUsuarioId(usuario.getId())
    .stream()                              // [1] transforma List em Stream
    .map(AssinaturaResponseDTO::fromEntity) // [2] converte Entity → DTO (para cada item)
    .toList();                             // [3] coleta resultado em List imutável
```

**Componentes de um pipeline:**
- **Intermediários** (lazy, não executam ainda): `.map()`, `.filter()`, `.sorted()`
- **Terminal** (dispara execução): `.toList()`, `.forEach()`, `.findFirst()`, `.collect()`

**Lazy evaluation**: o Stream só processa quando chega no terminal. Se você fizer
`.filter().map()` mas não chamar terminal, nada executa.

### "O que é Method Reference (`::`)?"

Atalho sintático para uma lambda que chama apenas um método.

```java
// Lambda:
.map(a -> AssinaturaResponseDTO.fromEntity(a))

// Method Reference equivalente (mais limpo):
.map(AssinaturaResponseDTO::fromEntity)
```

**Tipos no projeto:**
```java
AssinaturaResponseDTO::fromEntity  // Referência a método estático
usuario::getEmail                  // Referência a método de instância
```

### "O que é Optional e onde usam?"

`Optional<T>` representa "pode ter um valor, ou pode não ter". Evita `NullPointerException`.

```java
// No Repository:
Optional<Usuario> findByEmail(String email);

// No Service — tratando o Optional:
usuarioRepository.findByEmail(email)
    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
//  ↑ Se Optional vazio → lança a exceção
//    Se tem valor → retorna o valor

// No buscarPorIdDoUsuario — filter no Optional:
assinaturaRepository.findById(id)
    .filter(a -> a.getUsuario().getId().equals(usuario.getId()))  // valida propriedade
    .orElseThrow(() -> new IllegalArgumentException("Não encontrada."));
```

### "Explica os Records Java"

Records são classes **imutáveis** criadas com sintaxe mínima. Perfeitos para DTOs.

```java
// Declaração (bem simples):
public record AssinaturaRequestDTO(String nome, BigDecimal valor, CategoriaAssinatura categoria) {}

// O compilador gera automaticamente:
// → Construtor com todos os campos
// → Getters: dto.nome(), dto.valor(), dto.categoria()
// → equals(), hashCode(), toString()
// → Campos são private final (imutáveis)
```

---

## 5. INTEGRAÇÃO COM IA (Anthropic Claude)

### "Como funciona a integração com a IA?"

É um fluxo em **5 etapas**:

```
[1] Usuário clica em "Gerar Dicas"
[2] Frontend chama GET /api/ai/dicas (com JWT no header)
[3] AiController chama AiService.gerarDicas()
[4] AiService monta o prompt com dados reais do usuário
[5] Chama a API da Anthropic e retorna a resposta formatada
```

### "Como configuram o cliente HTTP para a Anthropic?"

`RestClientConfig.java` define um Bean Spring chamado `anthropicRestClient`:

```java
@Bean("anthropicRestClient")
public RestClient anthropicRestClient() {
    return RestClient.builder()
        .baseUrl("https://api.anthropic.com")
        .defaultHeader("x-api-key", apiKey)          // autenticação da API
        .defaultHeader("anthropic-version", "2023-06-01")
        .defaultHeader("Content-Type", "application/json")
        .build();
}
```

`@Qualifier("anthropicRestClient")` no AiService garante que Spring injeta
este Bean específico (não qualquer RestClient genérico).

### "O que é @Value e como usam?"

`@Value` injeta valores do `application.properties` diretamente em campos:

```java
@Value("${anthropic.model}")
private String model;  // "claude-haiku-4-5-20251001" — vem do .properties
```

**Por que usar .properties?** Para não hardcodar valores sensíveis (chave de API)
no código. Em produção, usaríamos variáveis de ambiente.

### "Como montas o prompt para a IA?"

O método `construirPrompt()` usa `StringBuilder` para montar um texto com dados
reais do usuário:

```java
sb.append("- Salário mensal: R$ ").append(formatar(resumo.salario())).append("\n");
sb.append("- Gasto total mensal: R$ ").append(formatar(resumo.totalMensal())).append("\n");
sb.append("- Percentual comprometido: ").append(resumo.percentualDoSalario()...).append("%\n");
// Adiciona todas as assinaturas ativas e gastos por categoria
```

A IA recebe contexto financeiro real e gera uma dica personalizada para aquele usuário.

### "O que é @JsonProperty e por que usam?"

A API da Anthropic espera o campo `max_tokens` em snake_case, mas Java usa camelCase.
`@JsonProperty` instrui o Jackson a serializar com o nome correto:

```java
public record AnthropicRequestDTO(
    String model,
    @JsonProperty("max_tokens")   // Java: maxTokens | JSON: max_tokens
    int maxTokens,
    @JsonProperty("system")
    String systemPrompt,
    List<AnthropicMessageDTO> messages,
    boolean stream
) {}
```

---

## 6. SOLID — ONDE ESTÁ NO CÓDIGO

### S — Single Responsibility (cada classe tem uma responsabilidade)

| Classe | Responsabilidade única |
|---|---|
| `AssinaturaService` | Regras de negócio de assinaturas |
| `JwtUtil` | Geração e validação de JWT |
| `AssinaturaRepository` | Acesso ao banco para assinaturas |
| `AssinaturaResponseDTO` | Transferência de dados de resposta |
| `JwtAuthFilter` | Filtro de autenticação JWT |

### O — Open/Closed (aberto para extensão, fechado para modificação)

O `GlobalExceptionHandler` é o melhor exemplo: para tratar um novo tipo de exceção,
basta **adicionar** um novo `@ExceptionHandler` — sem tocar nos existentes.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class) // já existe
    ...
    // Para novo tipo: só adicionar aqui, sem modificar os outros
    @ExceptionHandler(MinhaNovaException.class)
    ...
}
```

### L — Liskov Substitution (subtipos substituem o tipo base)

`UserDetailsServiceImpl` implementa `UserDetailsService` do Spring Security.
O Spring trata as duas formas como intercambiáveis — usa a interface em todo lugar
e nossa implementação é injetada automaticamente.

### I — Interface Segregation (interfaces específicas, não genéricas)

`AssinaturaRepository extends JpaRepository<Assinatura, Long>` — interface específica
para `Assinatura`. Não existe um repositório genérico que serve tudo.

### D — Dependency Inversion (depender de abstrações, não implementações)

Todos os Services recebem dependências via **construtor**, usando interfaces:

```java
// AuthService depende de interfaces, não de implementações:
private final PasswordEncoder passwordEncoder;        // interface
private final UserDetailsService userDetailsService;  // interface
// Spring injeta BCryptPasswordEncoder e UserDetailsServiceImpl em tempo de execução
```

---

## 7. FRONTEND ANGULAR

### "O que são Signals e por que usam?"

Signals são o novo sistema de reatividade do Angular. Um signal é um valor que,
quando muda, notifica automaticamente quem depende dele — sem necessidade de
Subject/BehaviorSubject do RxJS para casos simples.

```typescript
// AuthService — estado reativo do login:
private readonly _loggedIn = signal(false);
readonly loggedIn = this._loggedIn.asReadonly();

// Para mudar:
this._loggedIn.set(true);

// Para ler:
authService.loggedIn()  // chama como função
```

**`computed()`** — signal derivado, calculado automaticamente quando a dependência muda:

```typescript
// DashboardComponent:
readonly percentualClass = computed(() => {
    const p = this.resumo()?.percentualDoSalario ?? 0;
    if (p > 50) return 'text-danger';
    if (p > 30) return 'text-warning';
    return 'text-success';
});
```

### "Por que @if e @for em vez de *ngIf e *ngFor?"

`*ngIf` e `*ngFor` são diretivas estruturais antigas — foram **depreciadas** no Angular 17.
O Built-in Control Flow (`@if`, `@for`) é mais performático e legível:

```html
<!-- PROIBIDO no projeto (depreciado): -->
<div *ngIf="lista">...</div>
<div *ngFor="let item of lista">...</div>

<!-- CORRETO (Built-in Control Flow): -->
@if (lista()) {
  <div>...</div>
}
@for (item of lista(); track item.id) {
  <div>{{ item.nome }}</div>
}
```

### "O que é um Guard e como funciona?"

Guard protege rotas — impede que usuários não autorizados acessem certas páginas.

```typescript
// auth.guard.ts
export const authGuard: CanActivateFn = () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (authService.loggedIn()) return true;  // logado → permite acesso
    return router.createUrlTree(['/login']);   // não logado → redireciona
};
```

```typescript
// app.routes.ts — aplicando o guard:
{ path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] }
```

### "O que é o Interceptor JWT do Angular?"

Intercepta **todas as requisições HTTP** e adiciona o token automaticamente.
Sem interceptor, teria que adicionar o header manualmente em cada chamada de service.

```typescript
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
    const token = localStorage.getItem('token');
    if (token) {
        const cloned = req.clone({
            setHeaders: { Authorization: `Bearer ${token}` }
        });
        return next(cloned);  // envia requisição modificada
    }
    return next(req);  // sem token: envia original
};
```

### "O que são Standalone Components?"

No Angular 19, componentes não precisam pertencer a um `NgModule`.
Cada componente declara suas próprias dependências no campo `imports`:

```typescript
@Component({
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink],
    ...
})
```

---

## 8. PADRÕES DE PROJETO — IDENTIFICAÇÃO RÁPIDA

| Padrão | Onde no projeto | Como identificar |
|---|---|---|
| **Repository** | `AssinaturaRepository`, `UsuarioRepository` | Interface que abstrai acesso ao banco |
| **DTO** | `AssinaturaRequestDTO`, `ResumoFinanceiroDTO` | Objetos de transferência entre camadas |
| **Factory Method** | `AssinaturaResponseDTO.fromEntity()` | Método estático que cria instâncias |
| **Builder** | `JwtUtil.generateToken()` (Jwts.builder()) | Construção fluente encadeada |
| **Chain of Responsibility** | Spring Security Filter Chain | Filtros encadeados (JWT → Security → Controller) |
| **Interceptor** | `JwtAuthFilter`, `jwtInterceptor` Angular | Intercepta requisições antes de processar |
| **Observer** | Angular Signals, `computed()` | Notificação automática de mudanças |
| **Strategy** | `PasswordEncoder` (BCrypt) | Interface com implementações intercambiáveis |

---

## 9. BANCO DE DADOS

### "Por que PostgreSQL?"

- **ACID**: Atomicidade, Consistência, Isolamento, Durabilidade — fundamental para dados financeiros
- **Suporte a NUMERIC(10,2)**: tipo preciso para valores monetários (sem imprecisão de float)
- **Open source e amplamente adotado** na indústria
- Integração nativa com JPA/Hibernate via driver JDBC

### "Como funciona o Flyway?"

Flyway gerencia **migrações de banco** — arquivos SQL versionados que criam/alteram tabelas
de forma controlada e rastreável.

```
V1__create_tables.sql → cria tabelas usuarios e assinaturas
V2__xxx.sql           → próxima migração (se houver alteração)
```

Na inicialização, o Spring Boot executa automaticamente as migrações que ainda não rodaram.

### "Explica o relacionamento entre Usuario e Assinatura"

```
Um usuário tem muitas assinaturas (1:N)

Usuario
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  List<Assinatura> assinaturas

Assinatura
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_id")
  Usuario usuario
```

`CascadeType.ALL` significa: se deletar um usuário, todas as assinaturas dele
são deletadas automaticamente (cascade delete).

---

## 10. PERGUNTAS-ARMADILHA — SAIBA RESPONDER

### "Por que vocês não usaram *ngFor?"
*ngFor foi depreciado no Angular 17. Adotamos o Built-in Control Flow (`@for`) que é
mais performático e é o padrão atual do Angular.

### "O que acontece se o token JWT expirar?"
O `JwtAuthFilter` lança `ExpiredJwtException`. O `GlobalExceptionHandler` captura e
retorna HTTP 401. O Angular `errorInterceptor` detecta o 401 e redireciona para o login.

### "Por que usar injeção via construtor e não @Autowired?"
Injeção via construtor é a forma recomendada pelo Spring Boot desde a versão 4.3:
- Permite testar a classe sem Spring (passa as dependências direto no construtor)
- Garante que a instância é criada somente se todas as dependências existirem
- Não tem estado mutável inesperado (@Autowired pode injetar depois da construção)

### "O que é DDD e onde aplicam?"
DDD = Domain-Driven Design. No projeto:
- **Entity**: `Usuario`, `Assinatura` — objetos com identidade (têm ID)
- **Value Object**: `CategoriaAssinatura`, `Role` — enums imutáveis sem identidade
- **Repository**: `AssinaturaRepository` — abstração para acesso a dados
- **Domain Service**: `AssinaturaService` — lógica de negócio que não cabe em uma entidade
- **Bounded Context**: cada Service cuida do seu domínio (Auth ≠ Assinatura ≠ Usuario)

### "O que é CORS e por que configuraram?"
CORS = Cross-Origin Resource Sharing. Navegadores bloqueiam requisições de um domínio
para outro por segurança. Como o Angular (localhost:4200) chama o Spring (localhost:8080),
precisamos configurar o CORS no Spring para permitir essa comunicação.

```java
configuration.setAllowedOrigins(List.of("http://localhost:4200"));
```

### "O que faz o GlobalExceptionHandler?"
Centraliza o tratamento de exceções de toda a aplicação. Com `@RestControllerAdvice`,
qualquer exceção não tratada em um Controller é capturada aqui e retorna um JSON
padronizado em vez de um stack trace.

```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<Map<String, Object>> handleIllegal(IllegalArgumentException ex) {
    return ResponseEntity.status(400).body(Map.of("error", ex.getMessage(), "code", 400));
}
```

### "Por que usam SLF4J e Logger em vez de System.out.println?"
- Logs têm **nível** (INFO, WARN, ERROR, DEBUG) — em produção, desliga DEBUG sem mudar código
- Logs têm **timestamp** e **contexto** (classe, linha)
- SLF4J é uma **fachada** — troca o backend de log (Logback, Log4j) sem mudar o código
- `System.out.println` não tem nível, não tem contexto, não tem como desligar seletivamente

---

## 11. GLOSSÁRIO DE BOLSO

| Termo | Em 10 palavras |
|---|---|
| JWT | Token assinado stateless para autenticação sem sessão no servidor |
| BCrypt | Hash lento unidirecional para senhas nunca reversível |
| JPA | API Java que mapeia objetos para tabelas relacionais |
| JPQL | SQL orientado a objetos Java, não a tabelas SQL |
| @Transactional | Agrupa operações banco em tudo-ou-nada atômico com rollback |
| AOP | Código cross-cutting executado via Proxy sem tocar na classe original |
| ThreadLocal | Variável com valor diferente por thread, base do SecurityContextHolder |
| BigDecimal | Aritmética decimal precisa essencial para valores monetários |
| Stream API | Pipeline funcional para transformar coleções sem loops manuais |
| Optional | Contêiner que representa valor presente ou ausente, evita NullPointerException |
| Signal | Valor reativo Angular que notifica dependentes automaticamente ao mudar |
| DTO | Objeto só para transferir dados entre camadas sem expor entidades |
| Factory Method | Método estático nomeado que encapsula criação de objetos |
| RestClient | Cliente HTTP fluente do Spring para consumir APIs externas |
| @Value | Injeta configuração do application.properties em campos Java |
| Flyway | Controle de versão para schema de banco via arquivos SQL |
| CORS | Política de segurança do navegador para requisições entre domínios |
| Built-in Control Flow | @if @for @switch — substituto moderno das diretivas Angular depreciadas |

---

## 12. SEQUÊNCIA DE RESPOSTA SUGERIDA

Quando o professor perguntar sobre uma feature, use esta estrutura:

1. **O que é** — define o conceito em 1 frase
2. **Por que usamos** — justifica a escolha técnica
3. **Como funciona** — fluxo passo a passo
4. **Onde está no código** — aponta o arquivo e método específico
5. **Exemplo** — código concreto do projeto

**Exemplo aplicado para JWT:**
> "JWT é um token stateless para autenticação [O QUE É]. Usamos porque não precisamos
> armazenar sessão no servidor, o que torna o sistema escalável [POR QUE].
> Funciona assim: o login gera o token, o cliente envia em cada requisição no header
> Authorization, e o JwtAuthFilter valida antes de chegar ao Controller [COMO FUNCIONA].
> A lógica de geração está em `JwtUtil.generateToken()` e a validação em
> `JwtAuthFilter.doFilterInternal()` [ONDE NO CÓDIGO]. Por exemplo, `generateToken()`
> usa o padrão Builder: `.subject().claim().expiration().signWith().compact()` [EXEMPLO]."

---

*GeriStreams — Guia de Arguição Técnica | Gerado em: Março 2026*
