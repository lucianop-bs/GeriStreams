Você é um desenvolvedor backend sênior do GeriStreams (Spring Boot 3, Java 21, PostgreSQL, Flyway).

O usuário quer criar uma nova entidade JPA com todas as camadas do backend.

## Passo 1 — Coletar Informações

Se o usuário não forneceu os detalhes abaixo, **pergunte antes de gerar qualquer código**:

1. **Nome da entidade** (ex: `Notificacao`, `Categoria`, `Pagamento`)
2. **Campos** — para cada campo: nome, tipo Java, obrigatório (NOT NULL) ou opcional
3. **Relacionamento** com `Usuario`: tem? É obrigatório?
4. **Acesso**: acessível pelo próprio usuário, ou apenas por ADMIN?

## Passo 2 — Descobrir estado atual das migrations

Execute: listar todos os arquivos em `backend/src/main/resources/db/migration/V*.sql`

O próximo número da migration é o maior V encontrado + 1.

## Passo 3 — Gerar na seguinte ordem

### 1. Entity JPA
Arquivo: `backend/src/main/java/com/projeto/model/{Nome}.java`

Regras:
- Lombok: `@Getter @Setter @NoArgsConstructor`
- `@Column(nullable = false/true, length = N)` em todos os campos
- `@ManyToOne(fetch = FetchType.LAZY)` para o relacionamento com Usuario
- `@Enumerated(EnumType.STRING)` para enums
- `LocalDateTime createdAt = LocalDateTime.now()` com `@Column(nullable = false, updatable = false)`

### 2. Migration Flyway
Arquivo: `backend/src/main/resources/db/migration/V{N}__create_{nome_tabela}.sql`

Regras:
- Colunas SQL devem corresponder EXATAMENTE às `@Column` da Entity acima
- `NOT NULL` onde `nullable = false`
- `VARCHAR(n)` onde `length = n`
- `NUMERIC(10,2)` para `BigDecimal`
- `BIGINT REFERENCES usuarios(id) ON DELETE CASCADE` para foreign key
- `TIMESTAMP NOT NULL DEFAULT NOW()` para `createdAt`
- `CREATE INDEX idx_{tabela}_{coluna} ON {tabela}({coluna})` para foreign keys

### 3. Repository
Arquivo: `backend/src/main/java/com/projeto/repository/{Nome}Repository.java`

Inclua: `findByUsuarioId(Long usuarioId)` se tiver relacionamento com Usuario.

### 4. Request DTO (Java record)
Arquivo: `backend/src/main/java/com/projeto/dto/{nome}/{Nome}RequestDTO.java`

Use Bean Validation: `@NotBlank`, `@NotNull`, `@Positive`, `@Size`, etc.

### 5. Response DTO
Arquivo: `backend/src/main/java/com/projeto/dto/{nome}/{Nome}ResponseDTO.java`

Inclua método estático: `public static {Nome}ResponseDTO fromEntity({Nome} entity)`

### 6. Service
Arquivo: `backend/src/main/java/com/projeto/service/{Nome}Service.java`

Regras:
- `@Service @RequiredArgsConstructor`
- Logger Slf4j: `LoggerFactory.getLogger({Nome}Service.class)`
- `@Transactional` em criar/atualizar/deletar
- Verificação de ownership se relacionado com Usuario

### 7. Controller
Arquivo: `backend/src/main/java/com/projeto/controller/{Nome}Controller.java`

Regras:
- `@RestController @RequestMapping("/api/{nomes}")`
- `@Tag`, `@Operation`, `@SecurityRequirement(name = "bearerAuth")`
- `@Valid` em todos os `@RequestBody`
- `@PreAuthorize("hasRole('ADMIN')")` se for admin-only
- Retornar sempre DTO, nunca Entity

## Passo 4 — Validação Final

Após gerar todos os arquivos, confirme:
- [ ] Número da migration está correto (V{maior+1})
- [ ] Colunas SQL correspondem às anotações JPA
- [ ] Nenhuma Entity é retornada pelo Controller
- [ ] `@Valid` presente em todos os `@RequestBody`
- [ ] `@Transactional` em mutations do Service

## Lembrete
Após criar os arquivos, reinicie o backend para a migration ser executada pelo Flyway.
