# Claude Code CLI — Guia do Projeto GeriStreams

Este guia explica como usar o Claude Code CLI para desenvolver o GeriStreams com máxima produtividade.

---

## Instalação

```bash
npm install -g @anthropic-ai/claude-code
```

Verifique a instalação:
```bash
claude --version
```

Configure sua API key (primeira vez):
```bash
claude
# O CLI vai pedir sua ANTHROPIC_API_KEY e guiar a configuração
```

---

## Uso Básico

### Modo Interativo (padrão)
```bash
# Navegar até o projeto
cd C:/Dev/Faculdade/GeriStreams

# Iniciar sessão interativa
claude
```

### Modo Não-Interativo (um prompt, uma resposta)
```bash
claude -p "explica o que faz o AssinaturaService"
claude -p "liste todos os endpoints da API"
claude --print "qual é a estrutura do banco de dados?"
```

### Continuar a última sessão
```bash
claude -c
claude --continue
```

### Usar modelo específico
```bash
claude --model claude-opus-4-6  # mais poderoso
claude --model claude-haiku-4-5-20251001  # mais rápido
```

---

## Slash Commands Built-in

Digite `/` dentro de uma sessão interativa para ver todos os comandos:

| Comando | O que faz |
|---|---|
| `/help` | Lista todos os comandos disponíveis |
| `/clear` | Limpa o histórico da conversa atual |
| `/compact` | Compacta o histórico (reduz tokens usados) |
| `/memory` | Gerencia a memória persistente (lembranças entre sessões) |
| `/cost` | Mostra o custo estimado da sessão atual |
| `/status` | Mostra status das ferramentas e configurações |
| `/review` | Aciona o agente code-reviewer manualmente |

---

## Comandos Customizados do GeriStreams

Estes comandos foram criados especificamente para este projeto. Use `/nome-do-comando` no chat:

### `/nova-entidade`
Cria uma nova entidade JPA com todas as camadas do backend.

```
/nova-entidade
```
O Claude vai perguntar o nome, campos e relacionamentos e gerar:
- Entity JPA com Lombok e anotações corretas
- Migration Flyway (próxima versão automática)
- Repository
- Request DTO (Java record + validações)
- Response DTO (com `fromEntity()`)
- Service com `@Transactional` e logger
- Controller com `@Valid`, `@Tag`, `@Operation`

**Exemplo de prompt:**
```
/nova-entidade
Nome: Notificacao
Campos: titulo (String, obrigatório), mensagem (String, obrigatório), lida (Boolean, padrão false)
Relacionamento: ManyToOne com Usuario
Acesso: autenticado (não admin)
```

---

### `/novo-componente`
Cria um novo componente Angular 21 completo.

```
/novo-componente
```
Gera (se necessário):
- Interface TypeScript (`models/`)
- Service Angular com `inject(HttpClient)`
- Component standalone com Signals
- Template com `@if`, `@for` e Bootstrap 5
- Rota em `app.routes.ts`

**Exemplo:**
```
/novo-componente
Nome: Notificacoes
Exibe: lista de notificações do usuário
Operações: marcar como lida, excluir
Proteção: authGuard
```

---

### `/nova-migration`
Cria uma nova migration Flyway com o número correto.

```
/nova-migration
```
O Claude identifica a versão atual, gera o SQL com tipos PostgreSQL corretos e valida o alinhamento com a Entity JPA.

**Exemplo:**
```
/nova-migration
Preciso adicionar a coluna "descricao" (VARCHAR 255, nullable) na tabela assinaturas
```

---

### `/debug-backend`
Diagnostica erros do Spring Boot.

```
/debug-backend
```
Cole o stack trace e o Claude identifica:
- Erros de Flyway (migration duplicada, checksum alterado)
- Erros de schema Hibernate (`ddl-auto=validate`)
- Erros JWT (401/403)
- `NullPointerException`, `LazyInitializationException`, etc.
- Erros de CORS

**Exemplo:**
```
/debug-backend
Erro: org.hibernate.tool.schema.spi.SchemaManagementException: Schema-validation: missing table [notificacoes]
```

---

### `/full-stack`
Cria uma feature completa end-to-end (backend + frontend).

```
/full-stack
```
Combina `/nova-entidade` + `/novo-componente` em um fluxo unificado.

**Exemplo:**
```
/full-stack
Feature: sistema de alertas quando uma assinatura vai vencer em 7 dias
```

---

## Agents Especializados

Agents são subprocessos autônomos com ferramentas e contexto específicos. Eles são acionados automaticamente quando relevante, ou você pode referenciá-los explicitamente.

### `code-reviewer`
Revisor de código com checklist completo para o stack do projeto.

**Uso:**
```
revise o código que acabei de escrever
faça uma revisão do último commit
```
Verifica: padrões Angular 21, camadas Spring Boot, segurança, DTOs, migrations.

---

### `full-stack-feature`
Orquestra a criação de features completas com processo de 11 passos.

**Uso:**
```
use o agente full-stack-feature para criar o módulo de metas financeiras
```

---

### `migration-reviewer`
Valida migrations Flyway contra as entidades JPA.

**Uso:**
```
revise a migration que acabei de criar
o Spring Boot está falhando com SchemaValidationException, verifique as migrations
```

---

### `security-auditor`
Auditoria de segurança: JWT, CORS, autorização, exposição de dados.

**Uso:**
```
faça uma auditoria de segurança antes do deploy
verifique se há endpoints sem autenticação
```

---

## Fluxo de Trabalho Recomendado

### Criar uma nova feature

```bash
# 1. Iniciar sessão no projeto
cd C:/Dev/Faculdade/GeriStreams
claude

# 2. Usar o comando full-stack
/full-stack
Feature: [descreva o que precisa]

# 3. Revisar o código gerado
revise o código que acabei de criar

# 4. Executar e testar
# No terminal separado:
# cd backend && ./mvnw spring-boot:run
# cd frontend && npm start
```

### Debug de erro

```bash
claude
/debug-backend
[cole o stack trace completo aqui]
```

### Validar antes de commit

```bash
claude
use o agente security-auditor para revisar os arquivos que modifiquei
use o agente migration-reviewer para validar a nova migration
```

---

## Boas Práticas de Prompt

### Seja específico sobre o contexto
```
# Ruim
cria um controller

# Bom
cria o NotificacaoController em backend/src/main/java/com/projeto/controller/ com endpoints GET /api/notificacoes e PATCH /api/notificacoes/{id}/lida, seguindo o padrão do AssinaturaController
```

### Referencie arquivos existentes como modelo
```
cria um service de notificações seguindo o mesmo padrão do AssinaturaService, com os mesmos tipos de ownership checks
```

### Informe restrições explicitamente
```
cria o componente de notificações — lembra que não pode usar *ngIf nem *ngFor, só @if/@for, e precisa de standalone: true
```

### Peça validação após geração
```
depois de gerar, verifique se o SQL da migration está alinhado com as anotações @Column da Entity
```

---

## Dicas Avançadas

### Modo Plan (planejamento antes de executar)
```bash
claude --plan
# ou dentro do chat:
entrar em modo plan
```
O Claude apresenta o plano completo antes de qualquer alteração. Útil para features complexas.

### Compactar histórico longo
```
/compact
```
Compacta o histórico mantendo contexto essencial. Útil quando a sessão está longa.

### Memória persistente
```
# Salvar algo para futuras sessões
lembra que a porta do backend é 8080 e o swagger fica em /swagger-ui.html

# Ver o que está na memória
/memory
```

### Modo verbose (ver ferramentas sendo usadas)
```bash
claude --verbose
```

---

## Estrutura de Arquivos do Claude Code no Projeto

```
GeriStreams/
├── CLAUDE.md                          # Contexto principal do projeto
└── .claude/
    ├── settings.json                  # Permissões globais do projeto
    ├── settings.local.json            # Permissões locais (não commitado)
    ├── rules/
    │   ├── api-rules.md               # Regras Spring Boot (auto-aplicadas em *.java)
    │   ├── angular-rules.md           # Regras Angular 21 (auto-aplicadas em *.ts/*.html)
    │   └── java-rules.md              # Padrões Java detalhados (auto-aplicadas em *.java)
    ├── agents/
    │   ├── code-reviewer.md           # Revisor de código com checklist completo
    │   ├── full-stack-feature.md      # Criador de features completas
    │   ├── migration-reviewer.md      # Validador de migrations Flyway
    │   └── security-auditor.md        # Auditor de segurança
    ├── commands/
    │   ├── nova-entidade.md           # /nova-entidade — scaffolding backend
    │   ├── novo-componente.md         # /novo-componente — scaffolding Angular
    │   ├── nova-migration.md          # /nova-migration — migration Flyway
    │   ├── debug-backend.md           # /debug-backend — diagnóstico Spring Boot
    │   └── full-stack.md              # /full-stack — feature completa
    └── skills/
        └── resumos/
            └── resumao.md             # /resumao — documentação de código
```

---

## Recursos Adicionais

- [Documentação do Claude Code](https://docs.anthropic.com/claude-code)
- [Referência da API Anthropic](https://docs.anthropic.com/api)
- Swagger UI local: `http://localhost:8080/swagger-ui.html`
- Documentação do projeto: `docs/documentacao-completa.md`
- Guia de arguição: `docs/guia-arguicao.md`
