---
globs: backend/src/main/java/com/projeto/**/*.java
---

# Regras da API — Spring Boot 3

## Tratamento de Erros
- **Handler Global:** Todos os erros devem ser capturados pelo `GlobalExceptionHandler` anotado com `@RestControllerAdvice`.
- **Proibição:** É proibido usar blocos `try/catch` vazios ou que engolem a exceção sem relançá-la ou logar.
- **Exceções customizadas:** Lance `RuntimeException` com mensagem clara ou crie exceções específicas no pacote `exception/` quando necessário.

## Padrões de Resposta
- **Erros:** O `GlobalExceptionHandler` deve retornar sempre no formato `{ "error": "mensagem", "code": 400 }`.
- **Sucesso:** Retorne o DTO apropriado. `200 OK` para consultas, `201 Created` para criações, `204 No Content` para deleções.
- **Autenticação:** Todo endpoint que não seja `/api/auth/**` deve estar protegido via JWT no `SecurityConfig`.

## Observabilidade e Documentação
- **Logging:** Sempre logue erros com `logger.error("mensagem", e)` usando Slf4j. **NUNCA use `System.out.println`**.
- **OpenAPI/Swagger:** Todo novo endpoint deve ter `@Operation`, `@Tag` e `@SecurityRequirement(name = "bearerAuth")`.

## Regras para Geração de Código Backend
1. Siga o padrão de nomenclatura Java: `camelCase` para métodos/variáveis, `PascalCase` para classes.
2. O código deve ser modular e documentado para a arguição oral.
3. **Ao criar qualquer funcionalidade que altere o schema do banco, gere sempre a Migration Flyway SQL correspondente** (`VN__descricao.sql`).
4. **NUNCA exponha uma Entity JPA diretamente em um Controller** — sempre use DTOs.
5. `@Valid` é obrigatório em todos os `@RequestBody` dos Controllers.
6. `@Transactional` é obrigatório em todos os métodos de Service que fazem INSERT, UPDATE ou DELETE.
7. Use injeção por construtor (ou `@RequiredArgsConstructor` do Lombok) — nunca `@Autowired` em campo.
8. Request DTOs: use Java `record` com annotations do Bean Validation (`@NotBlank`, `@NotNull`, `@Positive`, etc.).
9. Response DTOs: use classe com método estático `fromEntity(Entity e)` para o mapeamento.
