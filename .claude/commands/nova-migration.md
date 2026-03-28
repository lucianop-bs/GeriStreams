Você é um especialista em Flyway e PostgreSQL para o projeto GeriStreams (Spring Boot 3, `ddl-auto=validate`).

## Passo 1 — Descobrir o próximo número de versão

Liste todos os arquivos em `backend/src/main/resources/db/migration/V*.sql`.

Identifique o maior número V. A nova migration será V{maior+1}.

## Passo 2 — Coletar informações

Se o usuário não especificou, pergunte:
1. O que a migration deve fazer? (criar tabela, adicionar coluna, criar índice, etc.)
2. Qual Entity JPA será afetada?

## Passo 3 — Ler a Entity JPA correspondente

Leia a Entity para extrair:
- Nome da tabela (via `@Table(name = "...")`)
- Todos os campos e suas anotações `@Column`
- Relacionamentos `@ManyToOne` / `@OneToMany`
- Tipos Java → mapeamento para PostgreSQL

## Passo 4 — Tabela de Mapeamento de Tipos

| Java / JPA | PostgreSQL |
|---|---|
| `Long` + `@GeneratedValue` | `BIGSERIAL PRIMARY KEY` |
| `Long` (FK) | `BIGINT REFERENCES outra_tabela(id) ON DELETE CASCADE` |
| `String` + `@Column(length = 100)` | `VARCHAR(100)` |
| `BigDecimal` + `@Column(precision=10,scale=2)` | `NUMERIC(10,2)` |
| `Boolean` | `BOOLEAN DEFAULT TRUE` |
| `Integer` | `INTEGER` |
| `LocalDateTime` (createdAt) | `TIMESTAMP NOT NULL DEFAULT NOW()` |
| `Enum` + `@Enumerated(EnumType.STRING)` | `VARCHAR(50) NOT NULL` |

## Passo 5 — Gerar a Migration

Arquivo: `backend/src/main/resources/db/migration/V{N}__{descricao}.sql`

Template para nova tabela:
```sql
-- V{N}: Descrição do que esta migration faz

CREATE TABLE IF NOT EXISTS {nome_tabela} (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    valor       NUMERIC(10, 2) NOT NULL,
    categoria   VARCHAR(50) NOT NULL,
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_{nome_tabela}_usuario_id ON {nome_tabela}(usuario_id);
```

Template para adicionar coluna:
```sql
-- V{N}: Adiciona coluna {coluna} à tabela {tabela}

ALTER TABLE {tabela}
    ADD COLUMN IF NOT EXISTS {coluna} VARCHAR(50) DEFAULT NULL;
```

## Passo 6 — Validação

Após gerar, verifique:
- [ ] Número V está correto (maior V existente + 1)
- [ ] Nome do arquivo: `VN__descricao_em_snake_case.sql` (2 underscores)
- [ ] Todas as colunas NOT NULL da Entity estão como `NOT NULL` no SQL
- [ ] Foreign keys têm índice correspondente
- [ ] Nenhuma coluna faltando em relação à Entity

## Lembrete
O Flyway executa automaticamente migrations novas ao reiniciar o backend. Certifique-se de que o backend esteja parado antes de criar o arquivo.
