# GeriStreams — Arquitetura, SOLID, DDD e Padrões de Design

> Documento técnico aprofundado. Cobre como o sistema funciona por baixo dos panos, onde cada princípio SOLID foi aplicado, conceitos de DDD, todos os padrões de design utilizados e diagramas de fluxo completos.

---

## Índice

1. [Diagrama Geral do Sistema](#1-diagrama-geral-do-sistema)
2. [Banco de Dados — ER Diagram e SQL](#2-banco-de-dados--er-diagram-e-sql)
3. [Arquitetura em Camadas](#3-arquitetura-em-camadas)
4. [SOLID — Aplicado ao Código Real](#4-solid--aplicado-ao-código-real)
5. [DDD — Domain-Driven Design](#5-ddd--domain-driven-design)
6. [Padrões de Design Utilizados](#6-padrões-de-design-utilizados)
7. [Como Funciona Por Baixo dos Panos](#7-como-funciona-por-baixo-dos-panos)
8. [Fluxos Completos com Diagramas de Sequência](#8-fluxos-completos-com-diagramas-de-sequência)
9. [Frontend — Angular Under the Hood](#9-frontend--angular-under-the-hood)

---

## 1. Diagrama Geral do Sistema

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        USUÁRIO (Browser)                                │
│                    localhost:4200 (Angular)                             │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │  HTTP/REST + JWT
                               │
┌──────────────────────────────▼──────────────────────────────────────────┐
│                     FRONTEND (Angular 19+)                              │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │  Components │  │   Services   │  │  Interceptor│  │   Guards    │  │
│  │ Dashboard   │  │ AuthService  │  │ jwtInterc.. │  │ authGuard   │  │
│  │ Subscript.  │  │ AssinService │  │ errorInterc.│  │ adminGuard  │  │
│  │ Admin       │  │ AiService    │  └─────────────┘  └─────────────┘  │
│  │ AiTips      │  │ UsuarioSvc   │                                      │
│  └─────────────┘  └──────────────┘                                      │
│          │  Signals (estado reativo)  │                                 │
│          └────────────────────────────┘                                 │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │  Authorization: Bearer <JWT>
                               │  Content-Type: application/json
                               │
┌──────────────────────────────▼──────────────────────────────────────────┐
│                    BACKEND (Spring Boot 3.2.5)                          │
│                         localhost:8080                                  │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              Spring Security Filter Chain                        │   │
│  │  CorsFilter → CsrfFilter(disabled) → JwtAuthFilter → Authz     │   │
│  └──────────────────────────────┬──────────────────────────────────┘   │
│                                 │                                       │
│  ┌──────────────────────────────▼──────────────────────────────────┐   │
│  │                    CONTROLLERS (REST)                            │   │
│  │  AuthController │ AssinaturaController │ UsuarioController      │   │
│  │  AdminController │ RelatorioController │ AiController           │   │
│  └──────────────────────────────┬──────────────────────────────────┘   │
│                                 │                                       │
│  ┌──────────────────────────────▼──────────────────────────────────┐   │
│  │                    SERVICES (Lógica de Negócio)                  │   │
│  │  AuthService │ UsuarioService │ AssinaturaService               │   │
│  │  RelatorioService │ AiService                                   │   │
│  └──────────────────────────────┬──────────────────────────────────┘   │
│                                 │                                       │
│  ┌──────────────────────────────▼──────────────────────────────────┐   │
│  │                    REPOSITORIES (DAO)                            │   │
│  │  UsuarioRepository │ AssinaturaRepository                       │   │
│  └──────────────────────────────┬──────────────────────────────────┘   │
│                                 │  JDBC / Hibernate ORM               │
└─────────────────────────────────┼───────────────────────────────────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────────────────┐
│                      PostgreSQL (porta 5432)                            │
│              Tabela: usuarios │ Tabela: assinaturas                     │
│                  Schema gerenciado pelo Flyway                          │
└─────────────────────────────────────────────────────────────────────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────────────────┐
│                   API Externa: Anthropic Claude                         │
│              https://api.anthropic.com/v1/messages                      │
│                    Modelo: claude-haiku-4-5                             │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Banco de Dados — ER Diagram e SQL

### Diagrama Entidade-Relacionamento (ER)

```
┌──────────────────────────────────┐
│           USUARIOS               │
├──────────────────────────────────┤
│ PK  id         BIGSERIAL         │
│     nome       VARCHAR(100)  NN  │
│     email      VARCHAR(150)  UQ  │
│     senha      VARCHAR(255)  NN  │  ← Hash BCrypt
│     salario    NUMERIC(10,2) NN  │  ← BigDecimal no Java
│     role       VARCHAR(20)   NN  │  ← 'USER' | 'ADMIN'
│     created_at TIMESTAMP     NN  │  ← Gerado automaticamente
└──────────────────────────────────┘
              │
              │ 1 : N
              │ (um usuário tem muitas assinaturas)
              │ ON DELETE CASCADE
              ▼
┌──────────────────────────────────┐
│           ASSINATURAS            │
├──────────────────────────────────┤
│ PK  id         BIGSERIAL         │
│     nome       VARCHAR(100)  NN  │  ← "Netflix", "Spotify"
│     valor      NUMERIC(10,2) NN  │  ← Custo mensal
│     categoria  VARCHAR(50)   NN  │  ← Enum: STREAMING_VIDEO, etc
│     ativo      BOOLEAN       NN  │  ← DEFAULT TRUE
│ FK  usuario_id BIGINT        NN  │  ← Referência a usuarios(id)
│     created_at TIMESTAMP     NN  │
└──────────────────────────────────┘

Legenda: PK = Primary Key | FK = Foreign Key | NN = NOT NULL | UQ = UNIQUE
```

### Script SQL (Flyway `V1__create_tables.sql`)

```sql
CREATE TABLE usuarios (
    id         BIGSERIAL     PRIMARY KEY,          -- Auto-incremento PostgreSQL
    nome       VARCHAR(100)  NOT NULL,
    email      VARCHAR(150)  NOT NULL UNIQUE,      -- Restrição de unicidade no banco
    senha      VARCHAR(255)  NOT NULL,             -- BCrypt precisa de 60+ chars
    salario    NUMERIC(10,2) NOT NULL DEFAULT 0.00,-- NUMERIC garante precisão exata
    role       VARCHAR(20)   NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE assinaturas (
    id         BIGSERIAL     PRIMARY KEY,
    nome       VARCHAR(100)  NOT NULL,
    valor      NUMERIC(10,2) NOT NULL,
    categoria  VARCHAR(50)   NOT NULL,
    ativo      BOOLEAN       NOT NULL DEFAULT TRUE,
    usuario_id BIGINT        NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    -- ON DELETE CASCADE: deletar usuário deleta TODAS suas assinaturas automaticamente
    created_at TIMESTAMP     NOT NULL DEFAULT NOW()
);
```

**Por que `NUMERIC(10,2)` e não `FLOAT` ou `DOUBLE`?**
```
FLOAT/DOUBLE: usa representação binária — imprecisão em base 10
  0.1 + 0.2 → 0.30000000000000004  ← ERRADO para dinheiro!

NUMERIC(10,2): armazenamento decimal exato
  0.1 + 0.2 → 0.30  ← CORRETO
  (10 dígitos no total, 2 após o ponto)
  Máximo: 99999999.99
```

**O que é Flyway e por que usar?**
```
Sem Flyway: você roda SQL manualmente em cada ambiente (dev, staging, prod)
            → Humano erra, esquece, ambientes ficam dessincronizados

Com Flyway: versionamento automático do schema do banco
            → V1__create_tables.sql (versão 1)
            → V2__add_column.sql (versão 2)
            → Flyway garante que cada script roda UMA VEZ
            → Ao iniciar a app: verifica qual versão está no banco
            → Se há scripts novos: executa em ordem
            → Idempotente: não corre o mesmo script duas vezes
```

---

## 3. Arquitetura em Camadas

```
┌─────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                       │
│  Controller  ←  HTTP Request  ←  Filtros  ←  Interceptores  │
│  - Valida entrada (@Valid)                                   │
│  - Mapeia URL para método Java                              │
│  - Retorna ResponseEntity com status HTTP correto           │
│  - NUNCA contém lógica de negócio                           │
│  - NUNCA acessa Repository diretamente                      │
└──────────────────────────┬──────────────────────────────────┘
                           │ delega 100%
┌──────────────────────────▼──────────────────────────────────┐
│                      BUSINESS LAYER                          │
│  Service (Business Object / BO)                             │
│  - TODA lógica de negócio fica aqui                        │
│  - Validações de negócio (email único, não auto-promoção)   │
│  - Cálculos (percentual financeiro, rankings)               │
│  - Orquestra chamadas a múltiplos Repositories              │
│  - Usa @Transactional para operações atômicas               │
│  - Converte Entity ↔ DTO                                   │
└──────────────────────────┬──────────────────────────────────┘
                           │ delega
┌──────────────────────────▼──────────────────────────────────┐
│                      PERSISTENCE LAYER                       │
│  Repository (Data Access Object / DAO)                      │
│  - APENAS operações de banco de dados                       │
│  - Sem lógica de negócio (nunca!)                           │
│  - Query Methods automáticos (Spring Data)                  │
│  - @Query customizadas (JPQL)                               │
│  - JpaRepository herda: save, find, delete, count...        │
└──────────────────────────┬──────────────────────────────────┘
                           │ SQL via Hibernate
┌──────────────────────────▼──────────────────────────────────┐
│                      DATABASE LAYER                          │
│  PostgreSQL — Schema gerenciado pelo Flyway                 │
└─────────────────────────────────────────────────────────────┘
```

**Regra de ouro: a dependência flui para baixo.**
- Controller depende de Service
- Service depende de Repository
- Repository depende do banco
- **Nunca ao contrário** (banco não chama Repository, Repository não chama Service)

---

## 4. SOLID — Aplicado ao Código Real

SOLID é um acrônimo com 5 princípios de design orientado a objetos criados por Robert C. Martin ("Uncle Bob").

---

### S — Single Responsibility Principle (Princípio da Responsabilidade Única)

> **Regra:** Uma classe deve ter apenas um motivo para mudar. Cada classe tem uma única responsabilidade.

**Onde foi aplicado no GeriStreams:**

| Classe | Única responsabilidade |
|--------|----------------------|
| `AuthService` | Autenticação (login + registro) — só muda se o processo de auth mudar |
| `UsuarioService` | Dados de usuário — só muda se as regras de usuário mudarem |
| `AssinaturaService` | Assinaturas — só muda se as regras de assinatura mudarem |
| `RelatorioService` | Geração de PDF — só muda se o formato do relatório mudar |
| `AiService` | Integração com IA — só muda se a integração com Anthropic mudar |
| `JwtUtil` | Operações JWT — só muda se o formato do token mudar |
| `JwtAuthFilter` | Filtro de autenticação — só muda se o fluxo de filtro mudar |
| `GlobalExceptionHandler` | Tratamento de erros — só muda se o formato de erro mudar |

**Exemplo de VIOLAÇÃO (o que NÃO foi feito):**
```java
// ❌ ERRADO — uma classe fazendo tudo:
public class UsuarioService {
    public void login() { ... }         // responsabilidade de Auth
    public void gerarPdf() { ... }      // responsabilidade de Relatório
    public void chamarIA() { ... }      // responsabilidade de AI
    public void buscarPerfil() { ... }  // responsabilidade de Usuario
}

// ✅ CORRETO — como está no projeto:
// Cada classe tem sua responsabilidade
public class AuthService    { public void login() { ... } }
public class RelatorioService { public byte[] gerarPdf() { ... } }
public class AiService      { public AiDicasResponseDTO gerarDicas() { ... } }
public class UsuarioService { public UsuarioResponseDTO buscarPerfil() { ... } }
```

**Por que importa?**
- Mudança no formato do PDF → altera apenas `RelatorioService`
- Mudança na API Anthropic → altera apenas `AiService`
- Mudança na lógica de JWT → altera apenas `JwtUtil`
- Classes menores são mais fáceis de testar e entender

---

### O — Open/Closed Principle (Princípio Aberto/Fechado)

> **Regra:** Classes devem ser **abertas para extensão, fechadas para modificação**. Adicionar funcionalidade via extensão, não alterando código existente.

**Onde foi aplicado no GeriStreams:**

**Exemplo 1 — `CategoriaAssinatura` (enum):**
```java
public enum CategoriaAssinatura {
    STREAMING_VIDEO,
    STREAMING_MUSICA,
    JOGOS,
    SOFTWARE,
    NOTICIAS,
    OUTRO  // ← fallback para categorias não listadas
}
```

Para adicionar uma categoria nova (ex: `EDUCACAO`), basta adicionar ao enum. O código existente no Service e Repository não precisa ser alterado — as queries `GROUP BY a.categoria` funcionam para qualquer categoria automaticamente.

**Exemplo 2 — `PasswordEncoder` como interface:**
```java
// SecurityConfig define a implementação
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(); // Implementação atual: BCrypt
}

// AuthService usa a INTERFACE, não a implementação
private final PasswordEncoder passwordEncoder; // ← Interface!

// Para trocar para Argon2 no futuro:
// Muda APENAS o Bean em SecurityConfig — AuthService não muda nada
@Bean
public PasswordEncoder passwordEncoder() {
    return new Argon2PasswordEncoder(); // Nova implementação
}
```

**Exemplo 3 — `UserDetailsService` como interface:**
```java
// AuthService depende da interface, não da implementação
private final UserDetailsService userDetailsService; // Interface do Spring

// UserDetailsServiceImpl implementa a interface
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String email) { ... }
}
// Se precisar mudar a fonte de usuários (LDAP, OAuth), basta criar nova implementação
```

---

### L — Liskov Substitution Principle (Princípio da Substituição de Liskov)

> **Regra:** Subclasses devem poder substituir suas superclasses sem quebrar o programa. Uma implementação de interface deve honrar o contrato da interface.

**Onde foi aplicado no GeriStreams:**

**Exemplo 1 — `JwtAuthFilter extends OncePerRequestFilter`:**
```java
public class JwtAuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) { ... }
}
```

O Spring usa `JwtAuthFilter` em lugar de qualquer `OncePerRequestFilter` sem saber detalhes da implementação. O Spring chama `doFilter(request, response)` — o `OncePerRequestFilter` garante que `doFilterInternal()` é chamado exatamente uma vez por requisição. Nossa implementação **respeita o contrato** (não quebra o comportamento esperado de um filtro).

**Exemplo 2 — `UserDetailsServiceImpl implements UserDetailsService`:**
```java
// Contrato da interface:
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}

// Nossa implementação respeita o contrato:
// ✓ Retorna UserDetails quando usuário existe
// ✓ Lança UsernameNotFoundException quando não existe (conforme especificado)
// ✓ Nunca lança exceção diferente da especificada
public class UserDetailsServiceImpl implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
            .map(u -> new User(u.getEmail(), u.getSenha(), List.of(...)))
            .orElseThrow(() -> new UsernameNotFoundException("..."));
    }
}
```

---

### I — Interface Segregation Principle (Princípio da Segregação de Interface)

> **Regra:** Clientes não devem ser forçados a depender de interfaces que não usam. Interfaces específicas são melhores que uma interface geral.

**Onde foi aplicado no GeriStreams:**

**Exemplo 1 — `UsuarioRepository` vs `AssinaturaRepository`:**

Em vez de uma única interface `Repository` com todos os métodos:
```java
// ❌ ERRADO — interface gorda:
public interface Repository {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Assinatura> findByUsuarioId(Long id);
    BigDecimal sumValorAtivo(Long id);
    // ... mistura tudo
}

// ✅ CORRETO — interfaces específicas:
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    // Apenas métodos relacionados a Usuario
}

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {
    List<Assinatura> findByUsuarioId(Long usuarioId);
    BigDecimal sumValorAtivoByUsuarioId(Long usuarioId);
    // Apenas métodos relacionados a Assinatura
}
```

`AuthService` usa `UsuarioRepository` — não precisa saber que `AssinaturaRepository` existe.
`AssinaturaService` usa ambos — cada um com seu escopo correto.

**Exemplo 2 — DTOs específicos por operação:**
```java
// ❌ Uma classe gigante para tudo:
class UsuarioDTO {
    String email; String senha;    // para login
    String nome; BigDecimal salario; // para registro
    Long id; String role; LocalDateTime createdAt; // para resposta
}

// ✅ Interfaces segregadas:
record LoginRequestDTO(String email, String senha) {}     // Apenas login
record RegisterRequestDTO(String nome, String email, ...) {} // Apenas registro
record UsuarioResponseDTO(Long id, String nome, ...) {}   // Apenas resposta (sem senha!)
record AtualizarSalarioDTO(BigDecimal salario) {}         // Apenas salário
```

Cada DTO expõe **apenas** o que aquela operação precisa.

---

### D — Dependency Inversion Principle (Princípio da Inversão de Dependências)

> **Regra:** Módulos de alto nível não devem depender de módulos de baixo nível. Ambos devem depender de abstrações (interfaces). Detalhes devem depender de abstrações, não o contrário.

**Onde foi aplicado no GeriStreams:**

```
                    DEPENDÊNCIA CORRETA (setas = "depende de")
┌─────────────────┐        ┌──────────────────────┐
│  AuthService    │───────▶│  PasswordEncoder      │ ← Interface (abstração)
│  (alto nível)   │        │  (interface Spring)   │
└─────────────────┘        └──────────────────────┘
                                      △ implementa
                            ┌─────────┴──────────┐
                            │ BCryptPasswordEncoder│ ← Detalhe (implementação)
                            └─────────────────────┘
```

**Injeção de dependências via construtor:**
```java
// AuthService declara dependências via interfaces/abstrações:
public class AuthService {
    private final UsuarioRepository usuarioRepository;  // Interface
    private final PasswordEncoder passwordEncoder;       // Interface
    private final JwtUtil jwtUtil;                       // Componente concreto, mas injetado
    private final AuthenticationManager authenticationManager; // Interface
    private final UserDetailsService userDetailsService; // Interface

    // Construtor: Spring injeta as implementações
    public AuthService(UsuarioRepository repo, PasswordEncoder enc, ...) { ... }
}
```

**O Spring como container de DI:**
```
Você declara:    "Preciso de um PasswordEncoder"
Spring resolve:  Qual bean implementa PasswordEncoder? BCryptPasswordEncoder!
Spring injeta:   new BCryptPasswordEncoder() no AuthService
```

Sem DI, o código seria:
```java
// ❌ Acoplamento forte (sem DI):
public class AuthService {
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // Fixo!
    // Para trocar o algoritmo, PRECISA alterar AuthService
}

// ✅ Com DI (como está no projeto):
public class AuthService {
    private final PasswordEncoder passwordEncoder; // Spring injeta
    // Para trocar: altera APENAS o @Bean em SecurityConfig
}
```

---

## 5. DDD — Domain-Driven Design

DDD é uma abordagem de desenvolvimento de software focada no **domínio do problema** (as regras de negócio) e não na infraestrutura técnica.

### Conceitos DDD Aplicados no GeriStreams

#### Entidades (Entities)

> Objetos com identidade única que persistem ao longo do tempo.

```java
// Usuario é uma Entidade — tem ID único, persiste, tem ciclo de vida
@Entity
public class Usuario {
    @Id private Long id;         // ← Identidade única
    private String email;        // ← Pode mudar ao longo do tempo
    private BigDecimal salario;  // ← Estado mutável
    private Role role;           // ← Estado mutável
}

// Assinatura é uma Entidade — tem ID próprio, pertence a um Usuario
@Entity
public class Assinatura {
    @Id private Long id;         // ← Identidade própria
    private Boolean ativo;       // ← Estado mutável (toggle)
    private BigDecimal valor;    // ← Estado mutável
}
```

**Características:** Têm `@Id`, sobrevivem a requisições, são persistidas no banco.

#### Objetos de Valor (Value Objects)

> Objetos sem identidade própria. Definidos pelo seu valor, não por um ID. Imutáveis.

```java
// Records Java = Value Objects naturais (imutáveis por definição)
public record AssinaturaRequestDTO(String nome, BigDecimal valor, CategoriaAssinatura categoria) {}
//                                 ↑ definido pelos seus campos, sem ID

public record ResumoFinanceiroDTO(
    BigDecimal salario,
    BigDecimal totalMensal,
    BigDecimal percentualDoSalario,
    List<AssinaturaResponseDTO> assinaturas,
    Map<String, BigDecimal> gastosPorCategoria
) {}
// Um ResumoFinanceiro é idêntico a outro se todos os campos forem iguais
// Não tem identidade própria — não é salvo no banco
```

**Características:** São Records Java (imutáveis), não têm `@Id`, não são Entities, representam dados transferidos.

#### Enums como Value Objects de Domínio

```java
// CategoriaAssinatura e Role são Value Objects do domínio
// Representam conceitos do negócio (não apenas strings)
public enum CategoriaAssinatura {
    STREAMING_VIDEO, STREAMING_MUSICA, JOGOS, SOFTWARE, NOTICIAS, OUTRO
}

public enum Role {
    USER,   // Usuário padrão do sistema
    ADMIN   // Administrador com acesso privilegiado
}
```

#### Repositórios (Repositories)

> Abstraem o mecanismo de persistência. O domínio não sabe se os dados vêm de PostgreSQL, MySQL ou arquivo.

```java
// O Service trata o Repository como uma "coleção" de Entidades
// Sem saber que por baixo há SQL, JDBC e PostgreSQL
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
}

// Para o Service, é como se fosse:
// List<Usuario> usuariosBanco = new ArrayList<>();
// usuariosBanco.stream().filter(u -> u.getEmail().equals(email)).findFirst();
// Mas na verdade executa: SELECT * FROM usuarios WHERE email = ?
```

#### Serviços de Domínio (Domain Services)

> Lógica de negócio que não pertence naturalmente a nenhuma Entidade.

```java
// AssinaturaService.calcularResumoFinanceiro() é um Serviço de Domínio:
// - Envolve Usuario (salário) + Assinaturas (valores)
// - Não pertence apenas ao Usuario nem apenas à Assinatura
// - É uma operação do DOMÍNIO (regra de negócio: percentual do salário)
public ResumoFinanceiroDTO calcularResumoFinanceiro() {
    // Orquestra Usuario + Assinatura para calcular resultado do domínio
}

// AuthService.registrar() é um Serviço de Domínio:
// - Regras: email único, senha hasheada, token gerado
// - Não pertence naturalmente à Entidade Usuario
```

#### Contexto Delimitado (Bounded Context)

O GeriStreams tem dois contextos delimitados principais:

```
┌─────────────────────────────────────────┐
│     CONTEXTO: GESTÃO DE USUÁRIOS        │
│  Usuario, Role, UsuarioRepository       │
│  AuthService, UsuarioService            │
│  Regras: unicidade email, roles         │
└─────────────────────────────────────────┘
                    │ usa
┌─────────────────────────────────────────┐
│     CONTEXTO: FINANÇAS DE ASSINATURAS   │
│  Assinatura, CategoriaAssinatura        │
│  AssinaturaRepository                   │
│  AssinaturaService, RelatorioService    │
│  AiService                              │
│  Regras: isolamento por usuário,        │
│          cálculo de percentual          │
└─────────────────────────────────────────┘
```

O contexto de "Usuários" não sabe de cálculo de percentual. O contexto de "Finanças" usa o Usuário como referência (para pegar salário), mas não gerencia autenticação.

---

## 6. Padrões de Design Utilizados

### 6.1 Repository Pattern

**Problema:** Acoplamento direto entre lógica de negócio e banco de dados.

**Solução:** Interface que abstrai o acesso a dados.

```
Service ──────▶ UsuarioRepository (interface)
                       △ implementado por
               SimpleJpaRepository (Spring Data, em runtime)
                       │
                   Hibernate ORM
                       │
                   PostgreSQL
```

```java
// Service não sabe como os dados são acessados:
public UsuarioResponseDTO buscarPerfil() {
    return UsuarioResponseDTO.fromEntity(getUsuarioAutenticado()); // Abstração!
}

// Repository esconde os detalhes:
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email); // Spring gera o SQL
}
```

### 6.2 DTO Pattern (Data Transfer Object)

**Problema:** Expor entidades JPA diretamente cria acoplamento entre a API e o banco.

**Solução:** Objetos intermediários que controlam o que é transferido.

```
[Frontend]                                              [Backend]
   │                                                       │
   │──── AssinaturaRequestDTO ────────────────────────────▶│
   │     { nome, valor, categoria }                        │ Controller
   │                                                       │
   │                                                       │ Service (cria Assinatura Entity)
   │                                                       │
   │                                                       │ Repository (salva no banco)
   │                                                       │
   │◀─── AssinaturaResponseDTO ────────────────────────────│
   │     { id, nome, valor, categoria, ativo, createdAt }  │
```

**Benefícios:**
- `UsuarioResponseDTO` **nunca** inclui o campo `senha` (segurança)
- O frontend não pode forçar um `usuarioId` ao criar assinatura (segurança)
- Mudança na estrutura do banco não quebra o contrato da API

**Implementação — Factory Method no DTO:**
```java
public record UsuarioResponseDTO(Long id, String nome, String email, BigDecimal salario, String role, LocalDateTime createdAt) {

    // Factory Method: converte Entity → DTO de forma controlada
    public static UsuarioResponseDTO fromEntity(Usuario usuario) {
        return new UsuarioResponseDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getSalario(),
            usuario.getRole().name(),
            usuario.getCreatedAt()
            // NUNCA incluímos usuario.getSenha() aqui!
        );
    }
}
```

### 6.3 Factory Method Pattern

**Problema:** Criação de objetos complexos espalhada pelo código.

**Solução:** Método estático que encapsula a lógica de criação.

```java
// O método fromEntity() em cada DTO é um Factory Method:
AssinaturaResponseDTO.fromEntity(assinatura) // ← Factory Method

// Sem ele, cada Service precisaria:
new AssinaturaResponseDTO(
    assinatura.getId(),
    assinatura.getNome(),
    assinatura.getValor().toString(),
    // ...lógica de conversão espalhada
)
// Com ele, a conversão está em UM lugar só
```

### 6.4 Builder Pattern

**Problema:** Construção de objetos complexos passo a passo.

**Solução:** API fluente encadeada.

```java
// JWT Builder — construção passo a passo:
return Jwts.builder()
    .subject(userDetails.getUsername())      // Passo 1: define subject
    .claim("roles", authorities)              // Passo 2: adiciona claim
    .issuedAt(new Date())                    // Passo 3: data de emissão
    .expiration(new Date(now + expiration))  // Passo 4: data de expiração
    .signWith(getSigningKey())               // Passo 5: assina com chave
    .compact();                              // Passo 6: constrói o token

// HTTP Response Builder:
return ResponseEntity.ok()                         // Passo 1: status 200
    .header(CONTENT_DISPOSITION, "attachment;...") // Passo 2: header
    .contentType(MediaType.APPLICATION_PDF)        // Passo 3: content-type
    .body(pdf);                                    // Passo 4: conteúdo

// RestClient Builder (RestClientConfig):
return RestClient.builder()
    .baseUrl(anthropicUrl)                         // URL base
    .defaultHeader("x-api-key", anthropicApiKey)   // Header padrão
    .defaultHeader("anthropic-version", version)   // Header padrão
    .defaultHeader("Content-Type", "application/json")
    .build();                                      // Cria o cliente
```

### 6.5 Strategy Pattern

**Problema:** Algoritmo deve ser intercambiável sem mudar quem o usa.

**Solução:** Interface que define o algoritmo; implementações concretas são substituíveis.

```java
// Strategy: PasswordEncoder
// Interface (estratégia):
public interface PasswordEncoder {
    String encode(CharSequence rawPassword);
    boolean matches(CharSequence rawPassword, String encodedPassword);
}

// Estratégia concreta atual:
BCryptPasswordEncoder // ← BCrypt com salt automático

// Estratégia alternativa possível (sem mudar AuthService):
Argon2PasswordEncoder  // ← Mais moderno
Pbkdf2PasswordEncoder  // ← Alternativa

// Quem usa a estratégia não precisa saber qual é:
private final PasswordEncoder passwordEncoder; // Só sabe que é um PasswordEncoder
passwordEncoder.encode(dto.senha()); // ← Chama a estratégia
```

### 6.6 Chain of Responsibility Pattern (Cadeia de Filtros)

**Problema:** Múltiplos handlers devem processar uma requisição em sequência.

**Solução:** Cada filtro processa e passa para o próximo.

```
Request HTTP
     │
     ▼
┌──────────────────────────────────────────────┐
│  Filter 1: CorsFilter                        │
│  → Adiciona headers CORS na resposta         │
│  → Passa para próximo: filterChain.doFilter()│
└──────────────────────────┬───────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────┐
│  Filter 2: CsrfFilter (desabilitado)         │
│  → Ignorado (csrf.disable())                 │
│  → Passa para próximo                        │
└──────────────────────────┬───────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────┐
│  Filter 3: JwtAuthFilter                     │
│  → Lê header Authorization                   │
│  → Valida token JWT                          │
│  → Popula SecurityContextHolder              │
│  → Passa para próximo: filterChain.doFilter()│
└──────────────────────────┬───────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────┐
│  Filter 4: AuthorizationFilter               │
│  → Verifica se rota permite o usuário atual  │
│  → Se não: 401 ou 403                       │
│  → Se sim: passa para Controller             │
└──────────────────────────┬───────────────────┘
                           │
                           ▼
                     Controller
```

```java
// No JwtAuthFilter, o padrão é explícito:
@Override
protected void doFilterInternal(request, response, filterChain) {
    // ... processa JWT ...
    filterChain.doFilter(request, response); // ← Passa para o próximo
}
```

### 6.7 Observer Pattern / Reactive (Angular Signals)

**Problema:** Quando o estado muda, como notificar automaticamente quem depende dele?

**Solução:** Signals em Angular — quem "assiste" o signal é notificado automaticamente.

```typescript
// Signal = Observable simplificado (síncrono)
readonly resumo = signal<ResumoFinanceiro | null>(null);

// computed() = "depende de resumo" — recalcula automaticamente quando resumo muda
readonly percentualClass = computed(() => {
    const p = this.resumo()?.percentualDoSalario ?? 0; // ← "assiste" resumo
    if (p >= 30) return 'danger';
    if (p >= 15) return 'warning';
    return 'success';
});

// Quando isso é executado:
this.resumo.set(novoResumo); // ← atualiza o signal

// Angular automaticamente:
// 1. Detecta que resumo mudou
// 2. Recalcula percentualClass (que depende de resumo)
// 3. Atualiza o template onde percentualClass() é usado
// SEM precisar de ChangeDetectorRef.detectChanges() ou async pipe
```

### 6.8 Interceptor Pattern (Angular)

**Problema:** Toda requisição HTTP precisa do header JWT, mas repetir em cada service é tedioso e propenso a erros.

**Solução:** Um único interceptor que processa TODAS as requisições.

```typescript
// Sem interceptor:
class AssinaturaService {
    listar() {
        const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
        return this.http.get(url, { headers }); // Repetindo em todo lugar
    }
}

// Com interceptor (como está no projeto):
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
    const token = inject(AuthService).getToken();
    if (token) {
        req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }
    return next(req); // Passa para o próximo handler
};

// Resultado: toda requisição tem o JWT automaticamente
class AssinaturaService {
    listar() {
        return this.http.get(url); // Limpo! JWT é adicionado automaticamente
    }
}
```

---

## 7. Como Funciona Por Baixo dos Panos

### 7.1 JPA/Hibernate — Ciclo de Vida de uma Entidade

O Hibernate gerencia entidades em diferentes **estados**:

```
┌────────────────────────────────────────────────────────────────┐
│                    ESTADOS JPA/HIBERNATE                        │
│                                                                 │
│  ┌─────────────┐    new Assinatura()   ┌─────────────────────┐ │
│  │  TRANSIENT  │─────────────────────▶│     MANAGED         │ │
│  │ (não rastr.)│   save() / persist() │  (rastreado pelo JPA)│ │
│  └─────────────┘                       └────────┬────────────┘ │
│                                                 │              │
│                                        flush() / commit()       │
│                                                 │              │
│                                                 ▼              │
│                                        ┌─────────────────────┐ │
│                                        │  BANCO DE DADOS     │ │
│                                        │  INSERT/UPDATE/     │ │
│                                        │  DELETE executado   │ │
│                                        └─────────────────────┘ │
│                                                                 │
│  ┌─────────────┐    evict() / clear()  ┌─────────────────────┐ │
│  │  DETACHED   │◀────────────────────── │     MANAGED         │ │
│  │ (fora do    │                        │                     │ │
│  │  contexto)  │                        └─────────────────────┘ │
│  └─────────────┘                                                │
└────────────────────────────────────────────────────────────────┘
```

**Por que `save()` tanto insere quanto atualiza?**
```java
Assinatura a = new Assinatura(); // Estado: TRANSIENT (sem ID)
a.setNome("Netflix");
assinaturaRepository.save(a);   // JPA: INSERT (gera ID)
// Agora: a.getId() = 5 (ID gerado pelo banco)

a.setNome("Netflix Premium");   // Ainda MANAGED (dentro da transação)
assinaturaRepository.save(a);   // JPA: UPDATE (tem ID = 5)
// O JPA sabe: "se tem ID → UPDATE, se não tem → INSERT"
```

**O que é `@Transactional`?**
```java
@Transactional
public AssinaturaResponseDTO criar(AssinaturaRequestDTO dto) {
    // Tudo dentro deste método executa em uma transação ÚNICA

    Assinatura a = new Assinatura();
    a.setNome(dto.nome());
    assinaturaRepository.save(a); // INSERT preparado, mas não confirmado

    // Se aqui der NullPointerException...
    // ROLLBACK: o INSERT é desfeito! O banco volta ao estado anterior.

    return AssinaturaResponseDTO.fromEntity(a); // Se chegou aqui: COMMIT
}
// Ao sair do método sem exceção: COMMIT (confirma no banco)
// Se exceção: ROLLBACK (desfaz tudo)
```

**Por que `FetchType.LAZY` em `@ManyToOne`?**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "usuario_id")
private Usuario usuario;
```

```
LAZY (padrão configurado):
  SELECT * FROM assinaturas WHERE id = 5
  → Retorna: Assinatura com usuario_id = 3
  → usuario NÃO é carregado ainda (apenas um proxy)
  → Só carrega quando você chama assinatura.getUsuario().getNome()

EAGER (se fosse configurado assim):
  SELECT a.*, u.* FROM assinaturas a JOIN usuarios u ON a.usuario_id = u.id WHERE a.id = 5
  → Carrega Assinatura E Usuario juntos imediatamente
  → Se você não precisa do Usuario, foi um JOIN desnecessário
```

LAZY é melhor para performance — carrega apenas quando necessário.

---

### 7.2 Spring Security — FilterChain Detalhado

**O que acontece quando uma requisição chega ao servidor:**

```
POST /api/auth/login (sem token — rota pública)
     │
     ▼
  CorsFilter
  → Verifica se a origem (localhost:4200) está na lista permitida
  → Adiciona headers CORS na resposta (Access-Control-Allow-Origin, etc)
  → Passa para o próximo
     │
     ▼
  JwtAuthFilter.doFilterInternal()
  → authHeader = request.getHeader("Authorization")
  → authHeader == null → NÃO tem Authorization header
  → filterChain.doFilter(request, response) → passa sem autenticar
     │
     ▼
  AuthorizationFilter
  → Rota: /api/auth/login
  → Configuração: .requestMatchers("/api/auth/**").permitAll()
  → PERMITE passar sem autenticação
     │
     ▼
  DispatcherServlet → AuthController.login()
```

```
GET /api/subscriptions (com token)
     │
     ▼
  CorsFilter → passa
     │
     ▼
  JwtAuthFilter.doFilterInternal()
  → authHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  → authHeader.startsWith("Bearer ") = TRUE
  → token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  → email = jwtUtil.extractEmail(token) = "joao@email.com"
  → SecurityContextHolder.getAuthentication() == null? SIM
  → userDetails = userDetailsService.loadUserByUsername("joao@email.com")
       └─ SELECT * FROM usuarios WHERE email = 'joao@email.com'
       └─ Retorna: new User(email, senhaHash, [ROLE_USER])
  → jwtUtil.isTokenValid(token, userDetails)?
       └─ extractEmail(token) == userDetails.getUsername()? SIM
       └─ !isTokenExpired(token)? SIM (dentro do prazo de 24h)
  → authToken = new UsernamePasswordAuthenticationToken(userDetails, null, [ROLE_USER])
  → SecurityContextHolder.getContext().setAuthentication(authToken) ← "usuário autenticado"
  → filterChain.doFilter() → passa autenticado
     │
     ▼
  AuthorizationFilter
  → Rota: /api/subscriptions
  → Configuração: .anyRequest().authenticated()
  → Tem autenticação no SecurityContextHolder? SIM
  → PERMITE passar
     │
     ▼
  DispatcherServlet → AssinaturaController.listar()
  → AssinaturaService.listar()
  → UsuarioService.getUsuarioAutenticado()
       └─ SecurityContextHolder.getContext().getAuthentication().getName()
       └─ Retorna: "joao@email.com"
       └─ usuarioRepository.findByEmail("joao@email.com")
       └─ SELECT * FROM usuarios WHERE email = 'joao@email.com'
```

---

### 7.3 JWT — Anatomia e Funcionamento Interno

**Estrutura de um JWT:**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2FvQGVtYWlsLmNvbSIsInJvbGVzIjoiW1JPTEVfVVNFUl0iLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6MTcwMDA4NjQwMH0.HMAC_SIGNATURE

─────────────┬─────────────  ────────────────────────────────────┬──────────────────────────  ──────────────┬──────────
             │                                                    │                                          │
           HEADER                                              PAYLOAD                                  SIGNATURE
    (algoritmo + tipo)                                    (dados/claims)                           (verificação)
```

**Decodificado (Base64URL):**

```json
// HEADER:
{
  "alg": "HS256",   // Algoritmo: HMAC-SHA256
  "typ": "JWT"      // Tipo: JSON Web Token
}

// PAYLOAD (claims):
{
  "sub": "joao@email.com",     // subject = email do usuário
  "roles": "[ROLE_USER]",      // papel do usuário
  "iat": 1700000000,           // issued at: quando foi gerado (Unix timestamp)
  "exp": 1700086400            // expiration: quando expira (+24h)
}

// SIGNATURE:
HMAC-SHA256(
  base64url(header) + "." + base64url(payload),
  SECRET_KEY   // "geristreams-secret-key-must-be-at-least-256-bits-long-for-hs256"
)
```

**Por que a assinatura é crítica?**
```
Atacante tenta modificar o token para se tornar ADMIN:
  Payload original: { "sub": "joao@email.com", "roles": "[ROLE_USER]" }
  Payload alterado: { "sub": "joao@email.com", "roles": "[ROLE_ADMIN]" }

  Para ser válido, precisaria gerar nova assinatura com HMAC-SHA256
  Mas HMAC-SHA256 requer a SECRET_KEY (que só o servidor tem)

  Resultado: signature inválida → JwtAuthFilter rejeita o token → 401
```

**Como `JwtUtil.parseClaims()` valida:**
```java
private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())  // ← RECALCULA a assinatura esperada
        .build()
        .parseSignedClaims(token)     // ← COMPARA com a assinatura recebida
        // Se não baterem: SignatureException → token rejeitado
        .getPayload();
}
```

---

### 7.4 Spring Bean Lifecycle — Como o Spring Gerencia Objetos

```
                    INICIALIZAÇÃO DA APLICAÇÃO
                             │
                             ▼
┌────────────────────────────────────────────────────────────┐
│  1. ComponentScan: Spring varre os pacotes               │
│     Encontra: @Service, @Repository, @Controller,        │
│               @Component, @Configuration                  │
└────────────────────────────┬───────────────────────────────┘
                             │
                             ▼
┌────────────────────────────────────────────────────────────┐
│  2. Criação dos Beans                                     │
│     Spring instancia cada classe encontrada              │
│     Ordem: primeiro as dependências, depois quem depende  │
│                                                           │
│     Exemplo:                                              │
│     1º UsuarioRepository (sem dependências)               │
│     2º PasswordEncoder (sem dependências)                 │
│     3º JwtUtil (sem dependências)                         │
│     4º UserDetailsServiceImpl (depende de UsuarioRepo)   │
│     5º AuthService (depende de todos acima)              │
└────────────────────────────┬───────────────────────────────┘
                             │
                             ▼
┌────────────────────────────────────────────────────────────┐
│  3. Injeção de Dependências                               │
│     Spring injeta dependências via construtor            │
│     new AuthService(usuarioRepo, passwordEncoder, ...)    │
└────────────────────────────┬───────────────────────────────┘
                             │
                             ▼
┌────────────────────────────────────────────────────────────┐
│  4. Aplicação pronta para receber requisições             │
│     Todos os Beans são Singletons (padrão)               │
│     Uma única instância de AuthService, UsuarioService... │
│     Thread-safe porque os campos são final (imutáveis)    │
└────────────────────────────────────────────────────────────┘
```

**Por que Singleton é seguro aqui?**
```java
// AuthService: campos finais, sem estado mutável
public class AuthService {
    private final UsuarioRepository usuarioRepository; // Final = imutável após construção
    private final PasswordEncoder passwordEncoder;      // Final = imutável
    // Não tem campos como: private Usuario usuarioAtual; ← PERIGOSO em Singleton!
}
// Uma instância compartilhada entre todas as threads é segura
// porque não há estado mutável (sem race conditions)
```

---

### 7.5 `@Value` e `@Configuration` — Como Propriedades São Injetadas

```
application.properties:
  anthropic.api.key=${ANTHROPIC_API_KEY:}
  anthropic.model=claude-haiku-4-5
           │
           │ Spring lê no startup
           ▼
  RestClientConfig:
    @Value("${anthropic.api.key}")
    private String anthropicApiKey; // ← "sk-ant-..."

  AiService:
    @Value("${anthropic.model}")
    private String model; // ← "claude-haiku-4-5"
```

**Fallback com `${VAR:valor_padrão}`:**
```properties
anthropic.api.key=${ANTHROPIC_API_KEY:}
# Se a variável de ambiente ANTHROPIC_API_KEY existir: usa ela
# Se não existir: usa string vazia (fallback)
# Em produção: define a variável de ambiente real
# Em dev: pode deixar vazio (sem IA funcional, mas app não quebra)
```

---

## 8. Fluxos Completos com Diagramas de Sequência

### 8.1 Fluxo de Registro (Register)

```
Angular                  Backend                  PostgreSQL
  │                        │                          │
  │──POST /api/auth/register──▶│                      │
  │  { nome, email, senha,  │                          │
  │    salario }            │                          │
  │                         │                          │
  │                    @Valid valida                   │
  │                    MethodArgumentNotValidException  │
  │                    → 400 se inválido               │
  │                         │                          │
  │                    AuthService.registrar()         │
  │                         │                          │
  │                         │──existsByEmail()────────▶│
  │                         │◀─ false ────────────────│
  │                         │                          │
  │                    Se true: throw IllegalArgumentException
  │                    GlobalExceptionHandler → 400    │
  │                         │                          │
  │                    new Usuario()                   │
  │                    BCrypt.encode(senha) → hash     │
  │                         │                          │
  │                         │──save(usuario)──────────▶│
  │                         │◀─ usuario com ID gerado ─│
  │                         │   INSERT INTO usuarios...│
  │                         │                          │
  │                    loadUserByUsername(email)        │
  │                         │──SELECT usuarios─────────▶│
  │                         │◀─ UserDetails ────────────│
  │                         │                          │
  │                    JwtUtil.generateToken()         │
  │                    JWT assinado com HMAC-SHA256     │
  │                         │                          │
  │◀─── 201 CREATED ────────│                          │
  │  { token, email, role } │                          │
  │                         │                          │
  │ localStorage.setItem(token)                        │
  │ _loggedIn.set(true)                                │
  │ navigate('/dashboard')                             │
```

### 8.2 Fluxo de Cálculo do Resumo Financeiro

```
Angular Dashboard            Backend                  PostgreSQL
      │                        │                          │
      │──GET /api/subscriptions/resumo──▶│               │
      │  Authorization: Bearer eyJ...    │               │
      │                         │                         │
      │               JwtAuthFilter:                      │
      │               1. extractEmail(token) = email     │
      │               2. loadUserByUsername(email)        │
      │                  └─SELECT FROM usuarios           │
      │               3. isTokenValid() = true           │
      │               4. SecurityContextHolder.set(auth)  │
      │                         │                         │
      │               AssinaturaController               │
      │               .resumoFinanceiro()                 │
      │                         │                         │
      │               AssinaturaService                  │
      │               .calcularResumoFinanceiro()         │
      │                         │                         │
      │               UsuarioService.getUsuarioAutenticado()
      │               SecurityContextHolder.getName()     │
      │               = "joao@email.com"                  │
      │                         │──SELECT usuarios────────▶│
      │                         │◀─ Usuario(id=3,sal=5000)─│
      │                         │                         │
      │                         │──findByUsuarioIdAndAtivoTrue(3)──▶│
      │                         │◀─ [Netflix, Spotify, Disney+]────│
      │                         │   SELECT * FROM assinaturas      │
      │                         │   WHERE usuario_id=3 AND ativo=true
      │                         │                         │
      │                         │──sumValorAtivoByUsuarioId(3)──▶│
      │                         │◀─ BigDecimal(385.50) ──────────│
      │                         │   SELECT SUM(valor) FROM ...    │
      │                         │                         │
      │                         │──sumValorGroupedByCategoria(3)─▶│
      │                         │◀─ [[STREAMING_VIDEO,250.00],   │
      │                         │    [STREAMING_MUSICA,80.00]...] │
      │                         │                         │
      │               Calcula percentual:                 │
      │               385.50 / 5000 * 100 = 7.71%         │
      │               (com HALF_UP rounding)              │
      │                         │                         │
      │               Monta ResumoFinanceiroDTO           │
      │                         │                         │
      │◀─── 200 OK ─────────────│                         │
      │  { salario: 5000,       │                         │
      │    totalMensal: 385.50, │                         │
      │    percentualDoSalario: 7.71, ...}                │
      │                         │                         │
      │ resumo.set(response)                              │
      │ ↓                                                 │
      │ percentualClass computed() recalcula              │
      │ = 'success' (< 15%)                               │
      │ ↓                                                 │
      │ Template atualiza automaticamente                 │
      │ Badge fica verde                                  │
```

### 8.3 Fluxo de Geração de Dicas de IA

```
Angular AiTips       Backend            Anthropic API
    │                   │                    │
    │──GET /api/ai/dicas─▶│                  │
    │                   │                    │
    │               JwtAuthFilter valida     │
    │                   │                    │
    │               AiService.gerarDicas()   │
    │                   │                    │
    │               AssinaturaService        │
    │               .calcularResumoFinanceiro()
    │               (ver fluxo 8.2)         │
    │                   │                    │
    │               UsuarioService           │
    │               .getUsuarioAutenticado() │
    │               → nome: "João"          │
    │                   │                    │
    │               construirPrompt(nome, resumo):
    │               "Olá! Me chamo João.    │
    │                Salário: R$ 5000.00    │
    │                Gasto: R$ 385.50 (7.71%)
    │                Assinaturas: Netflix..."│
    │                   │                    │
    │               AnthropicRequestDTO {    │
    │                 model: "claude-haiku-4-5"
    │                 max_tokens: 1024       │
    │                 system: "Você é um consultor..."
    │                 messages: [user: prompt]
    │               }                        │
    │                   │──POST /v1/messages─▶│
    │                   │  x-api-key: sk-ant-...
    │                   │  anthropic-version: 2023-06-01
    │                   │                    │
    │                   │                    │ Claude processa
    │                   │                    │ ~2-5 segundos
    │                   │                    │
    │                   │◀─ AnthropicResponseDTO
    │                   │  { content: [{type:"text",
    │                   │    text: "**Análise:**\n\n..."}]}
    │                   │                    │
    │               response.extractText()   │
    │               → "**Análise:**\n\n..."  │
    │                   │                    │
    │◀── 200 OK ─────────│                   │
    │  { dicas: "**Análise:**\n\n..." }      │
    │                    │                   │
    │ marked.parse(dicas) → HTML             │
    │ [innerHTML] exibe no template          │
```

### 8.4 Fluxo de Exportação de PDF

```
Angular Dashboard     Backend            OpenPDF Library
    │                    │                    │
    │──GET /api/reports/pdf──▶│               │
    │                    │                    │
    │               JwtAuthFilter valida      │
    │                    │                    │
    │               RelatorioService          │
    │               .gerarRelatorioPdf()      │
    │                    │                    │
    │               UsuarioService            │
    │               .getUsuarioAutenticado()  │
    │               → Usuario(nome, email)   │
    │                    │                    │
    │               AssinaturaService         │
    │               .calcularResumoFinanceiro()
    │                    │                    │
    │               ByteArrayOutputStream out│
    │               Document doc (A4)        │
    │               PdfWriter.getInstance()  │
    │               doc.open()               │
    │                    │──adicionarCabecalho──▶│
    │                    │  "GeriStreams"        │
    │                    │  "João | joao@..."    │
    │                    │◀─────────────────────│
    │                    │──adicionarResumo─────▶│
    │                    │  Tabela: Salário, Total%
    │                    │◀─────────────────────│
    │                    │──adicionarTabela─────▶│
    │                    │  Netflix | VIDEO | 49.90
    │                    │◀─────────────────────│
    │                    │──adicionarRodape─────▶│
    │                    │◀─────────────────────│
    │               doc.close()               │
    │               out.toByteArray() → byte[]│
    │                    │                    │
    │◀── 200 OK ──────────│                   │
    │  Content-Type: application/pdf          │
    │  Content-Disposition: attachment;       │
    │  filename="relatorio-geristreams-2026-03-26.pdf"
    │  Body: [bytes do PDF]                   │
    │                    │                    │
    │ URL.createObjectURL(blob)               │
    │ createElement('a')                      │
    │ a.click() → Browser salva arquivo      │
    │ URL.revokeObjectURL() → limpa memória  │
```

---

## 9. Frontend — Angular Under the Hood

### 9.1 Signals — Como o Estado Reativo Funciona

Signals são o mecanismo de estado reativo do Angular 17+. Substituem `BehaviorSubject` e `ChangeDetectorRef`.

```
                    CICLO DE VIDA DOS SIGNALS

  signal(valor_inicial)
        │
        │ Cria um "nó reativo" interno
        │ Angular rastreia quem "lê" este signal
        ▼
  ┌─────────────────┐
  │   Signal<T>     │  ← Contém o valor atual
  │  valor: T       │  ← Lista de "assinantes" (templates, computed)
  └────────┬────────┘
           │
           │ .set(novoValor) ou .update(fn)
           ▼
  Angular detecta mudança:
  1. Invalida todos os computed() que dependem deste signal
  2. Agenda re-renderização dos templates que usam signal()
  3. Na próxima "detecção de mudanças": re-renderiza apenas os nós afetados

                    COMPUTED SIGNALS

  const percentualClass = computed(() => {
      const p = this.resumo()?.percentualDoSalario ?? 0; // ← LÊ resumo
      return p >= 30 ? 'danger' : p >= 15 ? 'warning' : 'success';
  });

  Angular rastreia: "percentualClass depende de resumo"

  Quando resumo.set(novoResumo):
  1. Angular vê que resumo mudou
  2. Invalida percentualClass (ficou "stale")
  3. Próxima leitura de percentualClass() → recalcula
  4. Template atualiza automaticamente
```

**Diferença entre `signal()` e `computed()`:**

```typescript
// signal: estado MUTÁVEL (você pode fazer .set())
readonly loading = signal(false);
this.loading.set(true);   // ✓ Permitido

// computed: estado DERIVADO (calculado, somente leitura)
readonly percentualClass = computed(() => {
    return this.resumo()?.percentualDoSalario >= 30 ? 'danger' : 'success';
});
this.percentualClass.set('warning'); // ❌ ERRO! computed é readonly
```

### 9.2 Standalone Components — Por que Sem NgModules

**Angular tradicional (antes de v14):**
```typescript
// ❌ Era necessário declarar em um NgModule:
@NgModule({
    declarations: [DashboardComponent],
    imports: [CommonModule, HttpClientModule],
    // ... boilerplate
})
export class AppModule { }
```

**Angular moderno com Standalone (como no projeto):**
```typescript
// ✅ Componente autocontido:
@Component({
    selector: 'app-dashboard',
    standalone: true,  // ← "Eu me gerencio"
    imports: [RouterModule, ReactiveFormsModule, CurrencyPipe],
    templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit { }
// Sem necessidade de NgModule!
```

**Vantagens:**
- Menos boilerplate
- Tree-shaking melhor (Angular sabe exatamente o que cada componente usa)
- Lazy loading mais simples
- Componentes mais fáceis de reutilizar

### 9.3 Built-in Control Flow — `@if`, `@for`, `@switch`

```html
<!-- ❌ DEPRECIADO: *ngIf e *ngFor (Angular clássico) -->
<div *ngIf="loading">Carregando...</div>
<div *ngFor="let item of items">{{ item.nome }}</div>

<!-- ✅ CORRETO (como no projeto): Built-in Control Flow -->
@if (loading()) {
    <div>Carregando...</div>
}

@for (item of assinaturas(); track item.id) {
    <div>{{ item.nome }}</div>
}
@empty {
    <div>Nenhuma assinatura encontrada</div>
}

@switch (percentualClass()) {
    @case ('danger') { <span class="text-danger">Crítico!</span> }
    @case ('warning') { <span class="text-warning">Atenção</span> }
    @default { <span class="text-success">OK</span> }
}
```

**Por que `track item.id` no `@for`?**
```
Sem track: Angular re-renderiza TODOS os itens ao menor mudança
           (remove todos do DOM, cria novos)

Com track item.id: Angular sabe qual item corresponde a qual elemento DOM
                   Quando lista muda: atualiza APENAS os itens que mudaram
                   Muito mais performático (O(n) → O(1) por item)
```

### 9.4 Functional Guards vs Class Guards

```typescript
// ❌ CLASSE (Angular antigo — mais verboso):
@Injectable()
export class AuthGuard implements CanActivate {
    constructor(private authService: AuthService, private router: Router) {}

    canActivate(): boolean | UrlTree {
        return this.authService.getToken()
            ? true
            : this.router.createUrlTree(['/login']);
    }
}

// ✅ FUNÇÃO (Angular moderno — como no projeto):
export const authGuard: CanActivateFn = () => {
    const authService = inject(AuthService);  // inject() funciona fora de constructor!
    const router = inject(Router);
    return authService.getToken() ? true : router.createUrlTree(['/login']);
};
```

### 9.5 Reactive Forms — Como Funcionam

```typescript
// FormBuilder cria a estrutura do formulário
readonly form = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    valor: [null, [Validators.required, Validators.min(0.01)]],
    categoria: ['', Validators.required]
});

// Cada campo é um FormControl com:
// - valor atual
// - lista de validators
// - estado: valid/invalid, pristine/dirty, touched/untouched
```

**Fluxo de submissão:**
```
Usuário clica "Salvar"
    │
    ▼
salvar() {
    if (this.form.invalid) return; // ← Verifica todos os validators
    //   nome.required? valor.min? categoria.required?
    //   Se qualquer um falhar: form.invalid = true → sai

    const payload = this.form.getRawValue(); // ← Extrai valores tipados
    // { nome: "Netflix", valor: 49.90, categoria: "STREAMING_VIDEO" }

    this.assinaturaService.criar(payload) // ← POST /api/subscriptions
        .subscribe({
            next: () => { ... },  // ← Sucesso: fecha formulário
            error: () => { ... }  // ← Erro: mantém formulário aberto
        });
}
```

### 9.6 HttpClient + Observables

```typescript
// listar() retorna Observable — não executa ainda!
listar(): Observable<AssinaturaResponse[]> {
    return this.http.get<AssinaturaResponse[]>(this.apiUrl);
}

// Só executa quando há um subscriber:
this.assinaturaService.listar().subscribe(assinaturas => {
    this.assinaturas.set(assinaturas); // ← Atualiza signal → atualiza template
});

// Fluxo interno do HttpClient:
// 1. Cria Observable com a configuração da requisição
// 2. Ao subscribir: dispara XMLHttpRequest
// 3. Quando resposta chega: deserializa JSON → AssinaturaResponse[]
// 4. Chama o callback next() com o array
// 5. Observable completa (unsubscribe automático para HTTP)
```

---

## Resumo Executivo — Onde Cada Conceito Foi Aplicado

| Conceito | Onde no GeriStreams |
|----------|-------------------|
| **S** (Single Responsibility) | Cada Service tem uma responsabilidade: Auth, Usuario, Assinatura, Relatorio, AI |
| **O** (Open/Closed) | `CategoriaAssinatura` enum extensível; `PasswordEncoder` substituível |
| **L** (Liskov) | `UserDetailsServiceImpl` honra contrato de `UserDetailsService`; `JwtAuthFilter` honra `OncePerRequestFilter` |
| **I** (Interface Segregation) | `UsuarioRepository` e `AssinaturaRepository` separados; DTOs específicos por operação |
| **D** (Dependency Inversion) | Todos os Services dependem de interfaces; DI via construtor em toda a aplicação |
| **Repository Pattern** | `UsuarioRepository`, `AssinaturaRepository` — abstração do banco |
| **DTO Pattern** | `*RequestDTO` (entrada), `*ResponseDTO` (saída), nunca entidades expostas |
| **Factory Method** | `.fromEntity()` em cada DTO |
| **Builder** | JWT builder, ResponseEntity builder, RestClient builder |
| **Strategy** | `PasswordEncoder` → `BCryptPasswordEncoder` intercambiável |
| **Chain of Responsibility** | Spring Security FilterChain |
| **Observer/Reactive** | Angular Signals (`signal`, `computed`), `errorInterceptor`, `jwtInterceptor` |
| **DDD — Entity** | `Usuario`, `Assinatura` com identidade (@Id) |
| **DDD — Value Object** | Todos os Records/DTOs, `CategoriaAssinatura`, `Role` |
| **DDD — Repository** | `UsuarioRepository`, `AssinaturaRepository` |
| **DDD — Domain Service** | `AssinaturaService.calcularResumoFinanceiro()`, `AuthService.registrar()` |
| **DDD — Bounded Context** | Contexto de Usuários vs Contexto de Finanças/Assinaturas |
| **Flyway Migrations** | `V1__create_tables.sql` — versionamento de schema |
| **JWT Stateless Auth** | Token HMAC-SHA256, sem sessão no servidor |
| **BCrypt** | Hashing unidirecional com salt automático para senhas |
| **JPQL** | Queries customizadas com `@Query` nos Repositories |
| **@Transactional** | Operações de escrita atômicas em Service |
| **LAZY Loading** | `@ManyToOne(fetch=LAZY)` — carrega apenas quando necessário |
| **GlobalExceptionHandler** | Tratamento centralizado de exceções (`@RestControllerAdvice`) |
| **OpenPDF** | PDF em memória com `ByteArrayOutputStream` — sem arquivo temporário |
| **Anthropic REST** | `RestClient` com headers padrão configurados em `RestClientConfig` |
