# GeriStreams 🎬

Aplicação web fullstack para **gerenciamento de assinaturas de streaming**, análise financeira personalizada e dicas de economia geradas por Inteligência Artificial (Claude AI da Anthropic).

---

## Índice

1. [Visão Geral](#visão-geral)
2. [Stack Tecnológica](#stack-tecnológica)
3. [Arquitetura](#arquitetura)
4. [Estrutura de Pastas](#estrutura-de-pastas)
5. [Fluxos Principais](#fluxos-principais)
6. [Casos de Uso (UC)](#casos-de-uso)
7. [API — Endpoints](#api--endpoints)
8. [Banco de Dados](#banco-de-dados)
9. [Segurança (JWT)](#segurança-jwt)
10. [Integração com Claude AI](#integração-com-claude-ai)
11. [Como Rodar Localmente](#como-rodar-localmente)
12. [Variáveis de Ambiente](#variáveis-de-ambiente)

---

## Visão Geral

O **GeriStreams** resolve um problema real: as pessoas assinam vários serviços digitais (Netflix, Spotify, Adobe, etc.) e perdem o controle de quanto gastam. A aplicação permite:

- Cadastrar todas as suas assinaturas em um único lugar
- Calcular automaticamente quanto do salário está comprometido
- Ver gastos agrupados por categoria (vídeo, música, jogos...)
- Receber dicas personalizadas de economia geradas pelo **Claude Opus 4.6**
- Exportar um relatório financeiro em PDF
- Administradores podem monitorar todos os usuários e ver um ranking dos serviços mais usados

---

## Stack Tecnológica

| Camada | Tecnologia | Versão | Por que foi escolhida |
|--------|-----------|--------|-----------------------|
| Backend | Java + Spring Boot | 3.2.5 / Java 21 | Framework robusto, mercado amplo, excelente ecosistema |
| ORM | Spring Data JPA + Hibernate | embutido | Abstração do SQL, consultas tipadas |
| Banco | PostgreSQL | 15+ | Banco relacional open-source, robusto |
| Migrations | Flyway | embutido | Controle de versão do schema do banco |
| Segurança | Spring Security + JWT | embutido + JJWT 0.12.5 | Autenticação stateless, ideal para SPAs |
| PDF | OpenPDF | 1.3.30 | Geração de relatórios em memória, licença LGPL |
| IA | Anthropic Claude Opus 4.6 | API REST | Modelo mais capaz para análise contextual |
| Documentação API | SpringDoc OpenAPI (Swagger) | 2.5.0 | Documentação interativa automática |
| Frontend | Angular | 19+ | SPA moderna, TypeScript nativo, ecosistema maduro |
| Estilos | Bootstrap 5 + Bootstrap Icons | 5.x | Responsividade rápida, componentes prontos |
| HTTP Client | Angular HttpClient + RxJS | embutido | Requisições HTTP reativas |

---

## Arquitetura

### Visão Macro

```
┌─────────────────────────────────────────────────────────┐
│                    BROWSER (Cliente)                    │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Angular SPA (porta 4200)           │   │
│  │                                                 │   │
│  │  Components ──► Services ──► HttpClient         │   │
│  │      │              │                           │   │
│  │   Templates      Interceptors                   │   │
│  │   (HTML/SCSS)   (JWT + Error)                   │   │
│  └─────────────────────────┬───────────────────────┘   │
└────────────────────────────┼────────────────────────────┘
                             │ HTTP/JSON (REST)
                             │ Authorization: Bearer <JWT>
┌────────────────────────────▼────────────────────────────┐
│              Spring Boot API (porta 8080)               │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │Controller│  │ Service  │  │Repository│             │
│  │  (REST)  │─►│ (Lógica) │─►│  (JPA)   │             │
│  └──────────┘  └──────────┘  └────┬─────┘             │
│                                   │                     │
│  ┌──────────────────┐  ┌──────────▼──────────┐        │
│  │  Spring Security │  │    PostgreSQL DB     │        │
│  │  JWT Auth Filter │  │   (porta 5432)       │        │
│  └──────────────────┘  └─────────────────────┘        │
│                                                         │
│  ┌──────────────────────────────────────────────┐      │
│  │         Anthropic API (Claude Opus 4.6)      │      │
│  │         https://api.anthropic.com            │      │
│  └──────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────┘
```

### Camadas do Backend (Clean Architecture)

```
HTTP Request
     │
     ▼
┌──────────────────────────────────────┐
│  Filter Layer (JwtAuthFilter)        │  ← Valida JWT antes de tudo
│  Extrai email do token → autentica   │
└──────────────────────┬───────────────┘
                       │
                       ▼
┌──────────────────────────────────────┐
│  Controller Layer                    │  ← Recebe HTTP, valida @Valid, delega
│  @RestController + @RequestMapping   │    Nunca contém lógica de negócio
│  AuthController, UsuarioController,  │
│  AssinaturaController, AdminController│
│  AiController, RelatorioController   │
└──────────────────────┬───────────────┘
                       │ chama
                       ▼
┌──────────────────────────────────────┐
│  Service Layer (Business Logic)      │  ← TODA a lógica fica aqui
│  @Service + @Transactional           │    Cálculos, validações, regras
│  AuthService, UsuarioService,        │
│  AssinaturaService, AiService,       │
│  RelatorioService                    │
└──────────────────────┬───────────────┘
                       │ chama
                       ▼
┌──────────────────────────────────────┐
│  Repository Layer (Data Access)      │  ← Apenas acesso ao banco
│  extends JpaRepository               │    Queries JPQL quando necessário
│  UsuarioRepository, AssinaturaRepo.  │
└──────────────────────┬───────────────┘
                       │ SQL gerado pelo Hibernate
                       ▼
┌──────────────────────────────────────┐
│  Database (PostgreSQL)               │
│  Tabelas: usuarios, assinaturas      │
│  Migrations gerenciadas pelo Flyway  │
└──────────────────────────────────────┘
```

### DTO Pattern — Por que nunca expor as Entities

```
Frontend ◄──► Controller ◄──► Service ◄──► Repository ◄──► DB
              usa DTO           converte       usa Entity
              (sem senha,       Entity ↔ DTO   (mapeamento
              sem dados         (fromEntity)    JPA direto)
              internos)
```

---

## Estrutura de Pastas

```
GeriStreams/
├── backend/                          ← Spring Boot (Maven)
│   ├── pom.xml                       ← Dependências Java
│   └── src/main/java/com/projeto/
│       ├── GeriStreamsApplication.java   ← Entry point
│       │
│       ├── model/                    ← Entidades JPA (mapeamento do banco)
│       │   ├── Usuario.java
│       │   ├── Assinatura.java
│       │   ├── Role.java             ← Enum: USER | ADMIN
│       │   └── CategoriaAssinatura.java  ← Enum de categorias
│       │
│       ├── repository/               ← Interfaces Spring Data JPA
│       │   ├── UsuarioRepository.java
│       │   └── AssinaturaRepository.java
│       │
│       ├── dto/                      ← Objetos de transferência (nunca expor Entity!)
│       │   ├── auth/                 ← Login, Register, JwtResponse
│       │   ├── usuario/              ← UsuarioResponse, AtualizarSalario
│       │   ├── assinatura/           ← AssinaturaRequest, AssinaturaResponse
│       │   ├── financeiro/           ← ResumoFinanceiro
│       │   ├── admin/                ← RankingAssinatura
│       │   └── ai/                   ← DTOs da API da Anthropic
│       │
│       ├── service/                  ← Lógica de negócio
│       │   ├── AuthService.java
│       │   ├── UsuarioService.java
│       │   ├── AssinaturaService.java
│       │   ├── AiService.java        ← Integração Claude
│       │   └── RelatorioService.java ← Geração de PDF
│       │
│       ├── controller/               ← Endpoints REST
│       │   ├── AuthController.java
│       │   ├── UsuarioController.java
│       │   ├── AssinaturaController.java
│       │   ├── AdminController.java
│       │   ├── AiController.java
│       │   └── RelatorioController.java
│       │
│       ├── security/                 ← Camada de segurança JWT
│       │   ├── JwtUtil.java          ← Gera e valida tokens
│       │   ├── JwtAuthFilter.java    ← Intercepta requisições
│       │   └── UserDetailsServiceImpl.java
│       │
│       └── config/                   ← Configurações Spring
│           ├── SecurityConfig.java   ← Regras de acesso
│           ├── CorsConfig.java       ← Permite Angular acessar a API
│           ├── OpenApiConfig.java    ← Swagger UI
│           ├── RestClientConfig.java ← Cliente HTTP para Anthropic
│           └── GlobalExceptionHandler.java ← Erros centralizados
│
│   └── src/main/resources/
│       ├── application.properties    ← Configurações da app
│       └── db/migration/
│           └── V1__create_tables.sql ← Migration Flyway
│
└── frontend/                         ← Angular 19 (standalone)
    ├── angular.json                  ← Configuração do build Angular
    ├── package.json                  ← Dependências npm
    └── src/
        ├── environments/
        │   ├── environment.ts        ← apiUrl para desenvolvimento
        │   └── environment.prod.ts   ← apiUrl para produção
        └── app/
            ├── app.ts               ← Componente raiz (<router-outlet>)
            ├── app.config.ts        ← Providers globais (HTTP, Router)
            ├── app.routes.ts        ← Definição de rotas com lazy loading
            │
            ├── models/              ← Interfaces TypeScript (espelham DTOs)
            │   ├── auth.model.ts
            │   ├── usuario.model.ts
            │   ├── assinatura.model.ts
            │   └── financeiro.model.ts
            │
            ├── services/            ← Comunicação com a API
            │   ├── auth.service.ts
            │   ├── assinatura.service.ts
            │   ├── usuario.service.ts
            │   ├── ai.service.ts
            │   └── relatorio.service.ts
            │
            ├── guards/              ← Proteção de rotas
            │   ├── auth.guard.ts    ← Exige login
            │   └── admin.guard.ts   ← Exige role ADMIN
            │
            ├── interceptors/        ← Middleware HTTP
            │   ├── jwt.interceptor.ts    ← Injeta token em toda requisição
            │   └── error.interceptor.ts  ← Trata 401/403 globalmente
            │
            └── components/          ← Telas da aplicação
                ├── login/
                ├── register/
                ├── navbar/
                ├── dashboard/        ← Resumo financeiro + dicas IA + PDF
                ├── subscriptions/    ← CRUD de assinaturas
                ├── admin/            ← Painel admin (usuários + ranking)
                └── ai-tips/          ← Card de dicas do Claude
```

---

## Fluxos Principais

### Fluxo 1 — Cadastro e Login (Autenticação JWT)

```
Usuário preenche formulário de registro
         │
         ▼
[Frontend] RegisterComponent.onSubmit()
         │ POST /api/auth/register { nome, email, senha, salario }
         ▼
[Backend] AuthController.registrar()
         │ @Valid → valida campos obrigatórios
         ▼
[Backend] AuthService.registrar()
         │ 1. Verifica se email já existe (existsByEmail)
         │ 2. Criptografa senha com BCrypt
         │ 3. Salva usuário no banco
         │ 4. Gera token JWT assinado com HMAC-SHA256
         ▼
[Backend] Retorna { token, tipo: "Bearer", email, role }
         │
         ▼
[Frontend] AuthService.storeSession()
         │ Salva token e role no localStorage
         ▼
[Frontend] Redireciona para /dashboard

--- A partir daqui, TODA requisição inclui ---
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
         │
         ▼
[Backend] JwtAuthFilter intercepta
         │ 1. Extrai token do header
         │ 2. Valida assinatura e expiração
         │ 3. Carrega usuário do banco
         │ 4. Coloca no SecurityContext
         ▼
[Backend] Controller recebe a requisição autenticada
```

### Fluxo 2 — Resumo Financeiro (Dashboard)

```
[Frontend] DashboardComponent.ngOnInit()
         │ Chama usuarioService.buscarPerfil() em paralelo
         │ Chama assinaturaService.resumoFinanceiro()
         ▼
[Backend] GET /api/subscriptions/resumo
         │
         ▼
[Backend] AssinaturaService.calcularResumoFinanceiro()
         │ 1. Pega usuário do SecurityContext
         │ 2. Busca assinaturas ativas do usuário (findByUsuarioIdAndAtivoTrue)
         │ 3. Soma total via SUM(valor) no banco
         │ 4. Calcula: percentual = (total / salário) × 100
         │ 5. Agrupa gastos por categoria (GROUP BY categoria)
         ▼
[Backend] Retorna ResumoFinanceiroDTO {
           salario, totalMensal, percentualDoSalario,
           assinaturas[], gastosPorCategoria{}
         }
         │
         ▼
[Frontend] Renderiza cards, barra de progresso e gráfico de categorias
         │ Cor da barra: verde (<15%), amarelo (15-30%), vermelho (>30%)
```

### Fluxo 3 — Dicas de IA (Claude)

```
[Frontend] Usuário clica em "Gerar Dicas"
         │
         ▼
[Frontend] AiTipsComponent.gerarDicas()
         │ GET /api/ai/dicas
         ▼
[Backend] AiController → AiService.gerarDicas()
         │ 1. Busca resumo financeiro do usuário (AssinaturaService)
         │ 2. Monta prompt personalizado com:
         │    - Nome do usuário
         │    - Salário, gasto total, percentual
         │    - Lista de cada assinatura ativa com valor
         │    - Gastos por categoria
         │ 3. Chama API da Anthropic via RestClient:
         │    POST https://api.anthropic.com/v1/messages
         │    Headers: x-api-key, anthropic-version: 2023-06-01
         │    Body: { model: "claude-opus-4-6", max_tokens: 1024,
         │            system: "Você é consultor financeiro...",
         │            messages: [{ role: "user", content: <prompt> }] }
         │ 4. Extrai texto do content[0].text da resposta
         ▼
[Backend] Retorna { dicas: "1. **Cancele** o serviço X..." }
         │
         ▼
[Frontend] Converte markdown → HTML (** → <strong>, listas → <ol>)
         │ Exibe no card com [innerHTML]
```

### Fluxo 4 — Exportar PDF (UC17)

```
[Frontend] Usuário clica em "Exportar PDF"
         │
         ▼
[Frontend] relatorioService.exportarPdf()
         │ GET /api/reports/pdf
         │ responseType: 'blob'   ← recebe bytes, não JSON
         ▼
[Backend] RelatorioController → RelatorioService.gerarRelatorioPdf()
         │ 1. Busca usuário autenticado e resumo financeiro
         │ 2. Cria Document OpenPDF em memória (ByteArrayOutputStream)
         │ 3. Adiciona: cabeçalho, resumo, tabela assinaturas,
         │              gastos por categoria, rodapé
         │ 4. Fecha o documento → bytes prontos
         ▼
[Backend] Retorna bytes com headers:
         Content-Type: application/pdf
         Content-Disposition: attachment; filename="relatorio-2025-03-25.pdf"
         │
         ▼
[Frontend] Cria URL temporária do Blob → cria <a> invisível → click → download
         URL.createObjectURL(blob) → revoga após download
```

### Fluxo 5 — Ranking Admin (UC16)

```
[Frontend] AdminComponent.ngOnInit()
         │ relatorioService.rankingServicos()
         │ GET /api/admin/ranking
         ▼
[Backend] AdminController.rankingServicos()  ← @PreAuthorize("hasRole('ADMIN')")
         │
         ▼
[Backend] AssinaturaService.rankingServicos()
         │ Query JPQL:
         │ SELECT a.nome, COUNT(a), SUM(a.valor), AVG(a.valor)
         │   FROM Assinatura a WHERE a.ativo = true
         │   GROUP BY a.nome ORDER BY SUM(a.valor) DESC
         ▼
[Backend] Retorna List<RankingAssinaturaDTO> [
           { nomeServico: "Netflix", totalAssinantes: 42,
             valorTotalMensal: 630.00, valorMedio: 15.00 }, ...
         ]
         │
         ▼
[Frontend] Tabela com pódio 🥇🥈🥉
```

---

## Casos de Uso

| # | Ator | Caso de Uso | Endpoint |
|---|------|-------------|----------|
| UC01 | Anônimo | Cadastrar conta | `POST /api/auth/register` |
| UC02 | Anônimo | Fazer login | `POST /api/auth/login` |
| UC03 | USER | Ver perfil | `GET /api/users/me` |
| UC04 | USER | Atualizar salário | `PUT /api/users/me/salario` |
| UC05 | USER | Listar assinaturas | `GET /api/subscriptions` |
| UC06 | USER | Adicionar assinatura | `POST /api/subscriptions` |
| UC07 | USER | Editar assinatura | `PUT /api/subscriptions/{id}` |
| UC08 | USER | Remover assinatura | `DELETE /api/subscriptions/{id}` |
| UC09 | USER | Ativar/desativar | `PATCH /api/subscriptions/{id}/toggle` |
| UC10 | USER | Ver resumo financeiro | `GET /api/subscriptions/resumo` |
| UC11 | USER | Gerar dicas com IA | `GET /api/ai/dicas` |
| UC12 | ADMIN | Listar usuários | `GET /api/admin/users` |
| UC13 | ADMIN | Ver usuário específico | `GET /api/admin/users/{id}` |
| UC14 | ADMIN | Ver assinaturas de usuário | `GET /api/admin/users/{id}/subscriptions` |
| UC15 | ADMIN | Promover usuário para Admin | `PATCH /api/admin/users/{id}/promote` |
| UC16 | ADMIN | Ranking de serviços | `GET /api/admin/ranking` |
| UC17 | USER | Exportar relatório PDF | `GET /api/reports/pdf` |

---

## API — Endpoints

### Autenticação (público)

```
POST /api/auth/register
Body: { "nome": "João", "email": "joao@email.com", "senha": "123456", "salario": 5000.00 }
Response 201: { "token": "eyJ...", "tipo": "Bearer", "email": "joao@email.com", "role": "USER" }

POST /api/auth/login
Body: { "email": "joao@email.com", "senha": "123456" }
Response 200: { "token": "eyJ...", "tipo": "Bearer", "email": "joao@email.com", "role": "USER" }
```

### Usuário (requer token)

```
GET  /api/users/me
Response 200: { "id": 1, "nome": "João", "email": "...", "salario": 5000.00, "role": "USER" }

PUT  /api/users/me/salario
Body: { "salario": 6000.00 }
Response 200: { ...usuario atualizado... }
```

### Assinaturas (requer token)

```
GET    /api/subscriptions
Response 200: [ { "id": 1, "nome": "Netflix", "valor": 39.90, "categoria": "STREAMING_VIDEO", "ativo": true } ]

POST   /api/subscriptions
Body: { "nome": "Spotify", "valor": 21.90, "categoria": "STREAMING_MUSICA" }
Response 201: { ...assinatura criada... }

PUT    /api/subscriptions/{id}
Body: { "nome": "Spotify Premium", "valor": 26.90, "categoria": "STREAMING_MUSICA" }
Response 200: { ...assinatura atualizada... }

DELETE /api/subscriptions/{id}
Response 204: (sem body)

PATCH  /api/subscriptions/{id}/toggle
Response 200: { ...assinatura com ativo invertido... }

GET    /api/subscriptions/resumo
Response 200: {
  "salario": 5000.00,
  "totalMensal": 150.00,
  "percentualDoSalario": 3.0,
  "assinaturas": [...],
  "gastosPorCategoria": { "STREAMING_VIDEO": 90.00, "STREAMING_MUSICA": 60.00 }
}
```

### IA (requer token)

```
GET /api/ai/dicas
Response 200: {
  "dicas": "1. **Considere cancelar** o serviço X pois representa 30% do total..."
}
```

### Relatório (requer token)

```
GET /api/reports/pdf
Response 200: <bytes do PDF>
Headers: Content-Type: application/pdf
         Content-Disposition: attachment; filename="relatorio-2025-03-25.pdf"
```

### Admin (requer token + role ADMIN)

```
GET   /api/admin/users
GET   /api/admin/users/{id}
GET   /api/admin/users/{id}/subscriptions
PATCH /api/admin/users/{id}/promote   → promove para ADMIN
GET   /api/admin/ranking              → ranking de serviços mais usados
```

### Formato de erro (todos os endpoints)

```json
{ "error": "mensagem descritiva", "code": 400 }
{ "error": "Erro de validação", "code": 400, "fields": { "email": "E-mail inválido" } }
{ "error": "E-mail ou senha inválidos.", "code": 401 }
{ "error": "Acesso negado.", "code": 403 }
```

---

## Banco de Dados

### Schema (Flyway V1)

```sql
-- Tabela de usuários do sistema
CREATE TABLE usuarios (
    id         BIGSERIAL    PRIMARY KEY,          -- auto-incremento
    nome       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,      -- único: não pode duplicar
    senha      VARCHAR(255) NOT NULL,             -- hash BCrypt, nunca texto puro
    salario    NUMERIC(10,2) NOT NULL DEFAULT 0,  -- precisão monetária
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Tabela de assinaturas (N para 1 com usuarios)
CREATE TABLE assinaturas (
    id         BIGSERIAL    PRIMARY KEY,
    nome       VARCHAR(100) NOT NULL,
    valor      NUMERIC(10,2) NOT NULL,
    categoria  VARCHAR(50)  NOT NULL,             -- enum armazenado como string
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    usuario_id BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

### Diagrama ER

```
┌──────────────────┐          ┌──────────────────────┐
│    USUARIOS      │          │     ASSINATURAS       │
├──────────────────┤          ├──────────────────────┤
│ id (PK)          │◄────┐    │ id (PK)               │
│ nome             │     └────│ usuario_id (FK)       │
│ email (UNIQUE)   │   1:N    │ nome                  │
│ senha            │          │ valor                 │
│ salario          │          │ categoria             │
│ role             │          │ ativo                 │
│ created_at       │          │ created_at            │
└──────────────────┘          └──────────────────────┘
```

### Como o Flyway funciona

O Flyway controla a versão do banco de dados automaticamente:
- Na primeira inicialização, detecta que o banco está vazio
- Executa `V1__create_tables.sql` e registra na tabela `flyway_schema_history`
- Nas próximas inicializações, vê que V1 já foi executado e não executa de novo
- Para adicionar mudanças no banco, crie `V2__nome_da_mudanca.sql`

---

## Segurança (JWT)

### O que é JWT (JSON Web Token)?

Um JWT é uma string em 3 partes separadas por ponto:

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiJqb2FvQGVtYWlsLmNvbSJ9 . assinatura
      HEADER                      PAYLOAD                      SIGNATURE
  (algoritmo)              (dados do usuário)              (HMAC-SHA256)
```

- **Header**: algoritmo usado (HS256)
- **Payload (Claims)**: `sub` (email), `roles`, `iat` (emitido em), `exp` (expira em)
- **Signature**: garante que o token não foi adulterado

### Fluxo de Autenticação

```
1. Login → backend gera JWT assinado com secret de 256+ bits
2. Frontend armazena no localStorage
3. Todo request inclui: Authorization: Bearer <token>
4. JwtAuthFilter intercepta → valida assinatura + expiração
5. Coloca usuário no SecurityContext → controllers ficam autenticados
6. Token expira em 24h (86400000ms) → usuário precisa logar novamente
```

### Por que JWT para APIs REST?

- **Stateless**: o servidor não armazena sessão. Cada request carrega sua própria identidade.
- **Escalável**: funciona com múltiplos servidores sem compartilhar sessão.
- **Seguro**: a assinatura HMAC-SHA256 garante integridade. Qualquer alteração invalida o token.

---

## Integração com Claude AI

### Modelo utilizado: `claude-opus-4-6`

O mais capaz da família Claude, excelente para análise contextual e geração de conteúdo personalizado.

### Como funciona a chamada

```java
// 1. Backend recebe GET /api/ai/dicas
// 2. Busca dados financeiros do usuário logado
// 3. Monta este prompt dinâmico:

"Olá! Me chamo João.

Minha situação financeira atual com assinaturas:
- Salário mensal: R$ 5000.00
- Gasto total mensal em assinaturas ativas: R$ 250.00
- Percentual do salário comprometido: 5.0%

Minhas assinaturas ativas:
- Netflix (STREAMING_VIDEO) — R$ 39.90/mês
- Spotify (STREAMING_MUSICA) — R$ 21.90/mês
..."

// 4. Envia para:
POST https://api.anthropic.com/v1/messages
Headers:
  x-api-key: sk-ant-api03-...
  anthropic-version: 2023-06-01
  Content-Type: application/json
Body:
  {
    "model": "claude-opus-4-6",
    "max_tokens": 1024,
    "system": "Você é um consultor financeiro...",
    "messages": [{ "role": "user", "content": "<prompt acima>" }],
    "stream": false
  }

// 5. Extrai content[0].text da resposta e retorna ao frontend
```

---

## Como Rodar Localmente

### Pré-requisitos

| Software | Versão mínima | Download |
|----------|--------------|---------|
| Java JDK | 21 | [adoptium.net](https://adoptium.net) |
| Maven | 3.9+ | [maven.apache.org](https://maven.apache.org) |
| PostgreSQL | 15+ | [postgresql.org](https://www.postgresql.org/download) |
| Node.js | 18+ | [nodejs.org](https://nodejs.org) |
| Angular CLI | 17+ | `npm install -g @angular/cli` |

### Passo 1 — Configurar o PostgreSQL

```sql
-- Conecte no psql ou PGAdmin e execute:
CREATE DATABASE geristreams;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE geristreams TO postgres;
```

> O Flyway vai criar as tabelas automaticamente na primeira execução.

### Passo 2 — Configurar a chave da API da Anthropic

Você precisa de uma conta em [console.anthropic.com](https://console.anthropic.com) e uma API Key.

**Windows (PowerShell):**
```powershell
$env:ANTHROPIC_API_KEY = "sk-ant-api03-SUA_CHAVE_AQUI"
```

**Linux/Mac:**
```bash
export ANTHROPIC_API_KEY="sk-ant-api03-SUA_CHAVE_AQUI"
```

**No IntelliJ IDEA:**
- Run → Edit Configurations → Environment variables → Adicionar `ANTHROPIC_API_KEY=sk-ant-...`

### Passo 3 — Iniciar o Backend

```bash
cd GeriStreams/backend

# Compilar e iniciar
mvn spring-boot:run

# Ou gerar o JAR e executar:
mvn clean package -DskipTests
java -jar target/geristreams-0.0.1-SNAPSHOT.jar
```

Aguarde a mensagem:
```
Started GeriStreamsApplication in X.XXX seconds
```

A API estará disponível em: `http://localhost:8080`

Swagger UI (documentação interativa): `http://localhost:8080/swagger-ui.html`

### Passo 4 — Iniciar o Frontend

```bash
cd GeriStreams/frontend

# Instalar dependências (apenas na primeira vez)
npm install

# Iniciar servidor de desenvolvimento
ng serve
```

A aplicação estará disponível em: `http://localhost:4200`

### Passo 5 — Criar o primeiro usuário Admin

Por padrão, todos os usuários se registram como `USER`. Para criar um admin, você pode:

**Opção A — SQL direto no banco:**
```sql
UPDATE usuarios SET role = 'ADMIN' WHERE email = 'seu@email.com';
```

**Opção B — Via Swagger:**
1. Registre um usuário normal
2. Faça login e copie o token
3. No Swagger, clique em "Authorize" e cole o token
4. Execute o endpoint admin para promover outro usuário

---

## Variáveis de Ambiente

| Variável | Obrigatória | Padrão | Descrição |
|----------|-------------|--------|-----------|
| `ANTHROPIC_API_KEY` | Sim | — | Chave da API do Claude (console.anthropic.com) |
| `spring.datasource.url` | Não | `jdbc:postgresql://localhost:5432/geristreams` | URL do banco |
| `spring.datasource.username` | Não | `postgres` | Usuário do banco |
| `spring.datasource.password` | Não | `postgres` | Senha do banco |
| `app.jwt.secret` | Não | valor padrão no properties | Secret para assinar JWT (troque em produção!) |

### application.properties — Referência completa

```properties
# Porta do servidor
server.port=8080

# Banco de dados PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/geristreams
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=validate   # validate = não altera o banco, apenas valida
spring.jpa.show-sql=true                 # exibe SQL no console (útil para debug)

# Flyway — migrations automáticas
spring.flyway.enabled=true

# JWT
app.jwt.secret=<string-de-256-bits-ou-mais>
app.jwt.expiration-ms=86400000           # 24 horas

# CORS — permite o Angular acessar a API
app.cors.allowed-origins=http://localhost:4200

# Anthropic Claude
anthropic.api.key=${ANTHROPIC_API_KEY:}  # lê da variável de ambiente
anthropic.model=claude-opus-4-6

# Swagger
springdoc.swagger-ui.path=/swagger-ui.html
```

---

## Testando a API com cURL

```bash
# 1. Registrar usuário
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nome":"João","email":"joao@test.com","senha":"123456","salario":5000}'

# 2. Fazer login (guarde o token retornado)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"joao@test.com","senha":"123456"}' | jq -r '.token')

# 3. Adicionar assinatura
curl -X POST http://localhost:8080/api/subscriptions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Netflix","valor":39.90,"categoria":"STREAMING_VIDEO"}'

# 4. Ver resumo financeiro
curl http://localhost:8080/api/subscriptions/resumo \
  -H "Authorization: Bearer $TOKEN"

# 5. Gerar dicas com IA
curl http://localhost:8080/api/ai/dicas \
  -H "Authorization: Bearer $TOKEN"

# 6. Baixar PDF
curl http://localhost:8080/api/reports/pdf \
  -H "Authorization: Bearer $TOKEN" \
  -o relatorio.pdf
```

---

## Padrões e Boas Práticas Adotadas

| Padrão | Onde é usado | Por que |
|--------|-------------|---------|
| **DTO Pattern** | Todas as camadas | Nunca expõe dados internos (senha, campos JPA) |
| **Repository Pattern** | Acesso ao banco | Separa lógica de dados da lógica de negócio |
| **Service Layer** | Toda lógica | Mantém controllers simples e testáveis |
| **JWT Stateless** | Autenticação | Sem sessão no servidor, escalável |
| **BCrypt** | Senhas | Hash com salt aleatório, resistente a rainbow tables |
| **Flyway Migrations** | Schema do banco | Versionamento rastreável e reproduzível |
| **Global Exception Handler** | Erros HTTP | Resposta de erro consistente em toda a API |
| **Lazy Loading** (Angular) | Rotas | Carrega JS do componente só quando necessário |
| **Interceptors** | HTTP | JWT e erros tratados centralmente, sem repetição |
| **Functional Guards** | Rotas | Proteção de rotas sem classes, mais simples |
| **Standalone Components** | Angular | Sem NgModules, mais modular e tree-shakeable |

---

*Desenvolvido como projeto acadêmico · Stack: Spring Boot 3 + Angular 19 + PostgreSQL + Claude AI*
