# GeriStreams
> Projeto da disciplina de **Programacao Web** — Professor **Daniel Lucena**
> **Instituto Federal de Goias (IFG) — Campus Luziania**
<div>
 <img width="1262" height="1118" alt="Captura de tela 2026-03-26 154731" src="https://github.com/user-attachments/assets/05678527-06ee-4683-8fc7-ed31ce342465" />
 <img width="2049" height="1507" alt="Captura de tela 2026-03-26 160222" src="https://github.com/user-attachments/assets/13b13827-55c6-49bf-8fd1-d1b28e8ab8fa" />
<div>

## Sobre o Projeto

O **GeriStreams** e uma aplicacao web fullstack que ajuda usuarios a gerenciar suas assinaturas de streaming (Netflix,
Spotify, etc.), calcular o impacto no salario e receber dicas de economia geradas por Inteligencia Artificial.

**Stack:** Spring Boot 3 (Java 21) + Angular 21 + PostgreSQL + Claude AI (Anthropic)

---

## Este projeto foi inteiramente desenvolvido com IA

Todo o codigo — backend, frontend, banco de dados, seguranca e documentacao — foi gerado em colaboracao com o **Claude
Code** (ferramenta CLI da Anthropic), utilizando o modelo **Claude Opus 4.6**.

O objetivo foi demonstrar como a **engenharia de prompts e regras contextuais** pode ser usada como metodologia real de
desenvolvimento de software.

---

## Como a IA foi utilizada

### CLAUDE.md — O briefing do projeto

Antes de qualquer codigo, criamos o arquivo [`CLAUDE.md`](./CLAUDE.md) na raiz do repositorio. Ele funciona como o *
*contexto permanente** que a IA le automaticamente a cada conversa. Nele definimos:

- Stack tecnologica (Angular 21, Spring Boot 3, PostgreSQL)
- Arquitetura em camadas exigida pelo professor (Entity > Repository > Service > DTO > Controller)
- Estrutura de pastas do backend e frontend
- Convencoes de codigo (PascalCase, camelCase)

Sem esse arquivo, a IA "esqueceria" tudo a cada nova sessao. O `CLAUDE.md` garante consistencia ao longo de todo o
desenvolvimento.

### Rules — Regras automaticas de codigo

No arquivo [`.claude/rules/api-rules.md`](./.claude/rules/api-rules.md), definimos regras que a IA aplica
automaticamente:

- Proibido `*ngIf`, `*ngFor` — usar `@if`, `@for` (Angular moderno)
- Obrigatorio usar **Signals** para estado reativo
- Erros tratados pelo `GlobalExceptionHandler` (proibido try/catch vazio)
- Gerar migration SQL para cada feature nova
- Proibido `console.log` — usar logger interno

Essas regras funcionam como **guardrails**: mesmo pedindo "crie o endpoint X", a IA ja sabe seguir os padroes corretos.

### Memory — Persistencia entre conversas

O Claude Code possui um sistema de memoria em arquivo que mantem decisoes entre sessoes. Isso evita repetir contexto e
garante que a IA lembre do dominio, da stack e das correcoes feitas em conversas anteriores.

### Prompts — Como pediamos as features

Usamos prompts estruturados e incrementais. Exemplo:

```
Crie o modulo completo de assinaturas:
- Entity com nome, valor, categoria enum, ativo boolean, FK usuario
- Migration Flyway
- Repository, DTOs, Service com logica de negocio, Controller REST
- Frontend: component com formulario, tabela, botoes de acao
- Usar @if/@for e Signals
```

### Fluxo de trabalho

```
Definir contexto (CLAUDE.md + rules)
    |
Planejar feature (prompt descritivo)
    |
IA gera codigo (backend + frontend + migration)
    |
Revisar e testar
    |
Corrigir com feedback → IA memoriza para proximas features
    |
Repetir
```

---

## Estrutura do Projeto

```
GeriStreams/
├── CLAUDE.md                        <- Contexto para a IA
├── .claude/rules/api-rules.md       <- Regras de codigo para a IA
│
├── backend/                         <- Spring Boot 3 (Java 21)
│   ├── model/                       <- Entidades JPA
│   ├── repository/                  <- Interfaces Spring Data
│   ├── dto/                         <- Objetos de transferencia
│   ├── service/                     <- Logica de negocio
│   ├── controller/                  <- Endpoints REST
│   ├── security/                    <- JWT (autenticacao)
│   └── config/                      <- Configuracoes Spring
│
└── frontend/                        <- Angular 21
    ├── components/                  <- Telas (login, dashboard, admin...)
    ├── services/                    <- Comunicacao com a API
    ├── models/                      <- Interfaces TypeScript
    ├── guards/                      <- Protecao de rotas
    └── interceptors/                <- JWT e tratamento de erros
```

---

## Funcionalidades

- Cadastro e login com JWT
- CRUD de assinaturas de streaming
- Dashboard com resumo financeiro (total, percentual do salario, por categoria)
- Dicas de economia geradas pelo Claude AI
- Exportacao de relatorio em PDF
- Painel admin (listar usuarios, ranking de servicos, promover admins)

---

## Endpoints da API

### Autenticacao (`/api/auth`) — Publico

| Metodo | Rota                 | Descricao             | Por que existe?                                                                                            |
|--------|----------------------|-----------------------|------------------------------------------------------------------------------------------------------------|
| `POST` | `/api/auth/register` | Cadastra novo usuario | Permite que qualquer pessoa crie uma conta. Retorna token JWT para login automatico apos cadastro.         |
| `POST` | `/api/auth/login`    | Autentica usuario     | Valida credenciais (email + senha) e retorna token JWT. Sem ele, nenhuma rota protegida pode ser acessada. |

### Usuario (`/api/users`) — Autenticado (JWT)

| Metodo | Rota                    | Descricao                        | Por que existe?                                                                                                                              |
|--------|-------------------------|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `GET`  | `/api/users/me`         | Retorna perfil do usuario logado | O frontend precisa exibir nome, email e salario no dashboard. Usa `/me` em vez de `/{id}` por seguranca (usuario so ve seus proprios dados). |
| `PUT`  | `/api/users/me/salario` | Atualiza salario                 | O calculo de percentual gasto depende do salario. Permite que o usuario atualize sem editar o perfil inteiro.                                |

### Assinaturas (`/api/subscriptions`) — Autenticado (JWT)

| Metodo   | Rota                             | Descricao                    | Por que existe?                                                                                                                |
|----------|----------------------------------|------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `GET`    | `/api/subscriptions`             | Lista assinaturas do usuario | Alimenta a tabela principal do frontend. Retorna tanto ativas quanto inativas para que o usuario tenha visao completa.         |
| `POST`   | `/api/subscriptions`             | Cria nova assinatura         | Endpoint de criacao (CRUD). Recebe nome, valor e categoria. A assinatura e criada como ativa por padrao.                       |
| `PUT`    | `/api/subscriptions/{id}`        | Atualiza assinatura          | Permite corrigir nome, valor ou categoria de uma assinatura existente. Service valida que o `{id}` pertence ao usuario logado. |
| `DELETE` | `/api/subscriptions/{id}`        | Remove assinatura            | Exclusao permanente. Retorna 204 (sem corpo) pois o recurso deixou de existir.                                                 |
| `PATCH`  | `/api/subscriptions/{id}/toggle` | Ativa/desativa assinatura    | Permite "pausar" uma assinatura sem deleta-la. Assinaturas inativas nao entram no calculo financeiro.                          |
| `GET`    | `/api/subscriptions/resumo`      | Resumo financeiro            | Retorna total gasto, percentual do salario e gastos por categoria. Alimenta o dashboard e os graficos do frontend.             |

### Inteligencia Artificial (`/api/ai`) — Autenticado (JWT)

| Metodo | Rota            | Descricao                     | Por que existe?                                                                                                                |
|--------|-----------------|-------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| `GET`  | `/api/ai/dicas` | Gera dicas de economia com IA | Envia o perfil financeiro do usuario ao Claude Haiku 4.5, que retorna dicas personalizadas de como economizar nas assinaturas. |

### Relatorios (`/api/reports`) — Autenticado (JWT)

| Metodo | Rota               | Descricao                | Por que existe?                                                                                                                       |
|--------|--------------------|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| `GET`  | `/api/reports/pdf` | Exporta relatorio em PDF | Gera um PDF com resumo completo (assinaturas, gastos, percentual). Util para o usuario guardar ou imprimir. Usa a biblioteca OpenPDF. |

### Administracao (`/api/admin`) — Somente ADMIN

| Metodo  | Rota                                  | Descricao                          | Por que existe?                                                                                                     |
|---------|---------------------------------------|------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `GET`   | `/api/admin/users`                    | Lista todos os usuarios            | Visao administrativa da base de usuarios. Permite monitorar quantos usuarios estao cadastrados.                     |
| `GET`   | `/api/admin/users/{id}`               | Busca usuario por ID               | Permite ao admin ver detalhes de um usuario especifico antes de tomar acoes (ex: promover).                         |
| `GET`   | `/api/admin/users/{id}/subscriptions` | Lista assinaturas de um usuario    | Permite ao admin ver as assinaturas de qualquer usuario para suporte ou auditoria.                                  |
| `PATCH` | `/api/admin/users/{id}/promote`       | Promove usuario para ADMIN         | Permite que um admin confie permissoes administrativas a outro usuario. Impede auto-promocao e promocao duplicada.  |
| `GET`   | `/api/admin/ranking`                  | Ranking de servicos mais populares | Mostra quais servicos sao mais assinados e qual o gasto total/medio. Util para analise de tendencias da plataforma. |

> Todas as rotas protegidas exigem o header `Authorization: Bearer <token>`. Rotas admin retornam **403 Forbidden** se o
> usuario nao tiver papel ADMIN.

---

## Como Rodar

### Pre-requisitos

- Java JDK 21+
- Maven 3.9+
- PostgreSQL 15+
- Node.js 18+
- Angular CLI (`npm install -g @angular/cli`)

### 1. Banco de dados

```sql
CREATE DATABASE geristreams;
```

> As tabelas sao criadas automaticamente pelo Flyway.

### 2. Configurar variaveis de ambiente

```bash
cd backend
cp .env.example .env
# Edite o .env com sua senha do PostgreSQL
```

### 3. Backend

Abra a pasta `backend/` no **IntelliJ IDEA** e execute a classe `GeriStreamsApplication.java` (botao Run ou Shift+F10).

> Se preferir terminal e tiver o Maven no PATH: `mvn spring-boot:run`

API disponivel em **http://localhost:8080**
Swagger em **http://localhost:8080/swagger-ui.html**

### 4. Frontend

```bash
cd frontend
npm install
ng serve
```

Aplicacao disponivel em **http://localhost:4200**

### 5. Criar admin (opcional)

```sql
UPDATE usuarios SET role = 'ADMIN' WHERE email = 'seu@email.com';
```

---

## Ferramentas de IA utilizadas

| Ferramenta            | Uso                                          |
|-----------------------|----------------------------------------------|
| **Claude Code** (CLI) | Geracao e edicao de todo o codigo            |
| **Claude Opus 4.6**   | Modelo principal para desenvolvimento        |
| **CLAUDE.md**         | Contexto arquitetural persistente            |
| **.claude/rules/**    | Regras automaticas de qualidade              |
| **Memory System**     | Persistencia de decisoes entre sessoes       |
| **Claude Haiku 4.5**  | Modelo usado na feature de dicas de economia |

---

*Projeto academico desenvolvido com auxilio de Inteligencia Artificial (Claude Code / Anthropic)*
*Disciplina: Programacao Web — Prof. Daniel Lucena — IFG Campus Luziania*
