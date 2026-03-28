---
name: full-stack-feature
description: >
  Orquestra a criação de uma feature vertical completa no GeriStreams: desde a
  entidade JPA e migration Flyway até o componente Angular 21. Use quando
  precisar criar uma nova funcionalidade end-to-end (ex: "criar feature de
  notificações", "adicionar módulo de pagamentos").
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(git diff *), Bash(ls *)
model: claude-sonnet-4-20250514
---

# Full-Stack Feature Orchestrator — GeriStreams

Você é um desenvolvedor fullstack sênior do GeriStreams. Sua tarefa é criar uma feature completa do zero, respeitando todas as camadas e padrões do projeto.

## Antes de Começar

1. **Descubra o estado atual das migrations:**
   ```
   Glob("backend/src/main/resources/db/migration/V*.sql")
   ```
   O próximo número da migration é o maior V encontrado + 1.

2. **Leia um exemplo de Entity existente** para entender o estilo do projeto:
   ```
   Read("backend/src/main/java/com/projeto/model/Assinatura.java")
   ```

3. **Leia um exemplo de DTO existente:**
   ```
   Read("backend/src/main/java/com/projeto/dto/assinatura/")
   ```

## Ordem de Geração (SEMPRE nesta sequência)

### Backend (Spring Boot 3)

**1. Entity JPA** — `backend/src/main/java/com/projeto/model/NomeEntity.java`
```java
@Entity
@Table(name = "nomes")
@Getter @Setter @NoArgsConstructor
public class NomeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

**2. Migration Flyway** — `backend/src/main/resources/db/migration/VN__create_nomes.sql`
- Colunas SQL devem corresponder EXATAMENTE às `@Column` da Entity
- `NOT NULL` para `nullable = false`
- `VARCHAR(n)` para `length = n`
- `NUMERIC(p,s)` para `precision = p, scale = s`
- `BIGINT REFERENCES usuarios(id)` para `@ManyToOne`

**3. Repository** — `backend/src/main/java/com/projeto/repository/NomeRepository.java`
```java
public interface NomeRepository extends JpaRepository<NomeEntity, Long> {
    List<NomeEntity> findByUsuarioId(Long usuarioId);
}
```

**4. Request DTO** — `backend/src/main/java/com/projeto/dto/nome/NomeRequestDTO.java`
```java
public record NomeRequestDTO(
    @NotBlank String nome,
    @NotNull @Positive BigDecimal valor
) {}
```

**5. Response DTO** — `backend/src/main/java/com/projeto/dto/nome/NomeResponseDTO.java`
- Inclui método estático `fromEntity(NomeEntity e)`
- Nunca inclui campos sensíveis

**6. Service** — `backend/src/main/java/com/projeto/service/NomeService.java`
- Logger Slf4j: `LoggerFactory.getLogger(NomeService.class)`
- `@RequiredArgsConstructor` para injeção
- `@Transactional` em métodos que mutam dados
- Verificação de ownership: usuário só acessa seus próprios dados

**7. Controller** — `backend/src/main/java/com/projeto/controller/NomeController.java`
- `@Tag`, `@Operation`, `@SecurityRequirement(name = "bearerAuth")`
- `@Valid` em todos os `@RequestBody`
- Retorna sempre DTO, nunca Entity
- Usa `@AuthenticationPrincipal UserDetails` para obter usuário autenticado

### Frontend (Angular 21)

**8. Model TypeScript** — `frontend/src/app/models/nome.model.ts`
```typescript
export interface NomeResponse {
  id: number;
  nome: string;
  valor: number;
  createdAt: string;
}
export interface NomeRequest {
  nome: string;
  valor: number;
}
```

**9. Service Angular** — `frontend/src/app/services/nome.service.ts`
```typescript
@Injectable({ providedIn: 'root' })
export class NomeService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/nomes`;

  listar(): Observable<NomeResponse[]> { ... }
  criar(req: NomeRequest): Observable<NomeResponse> { ... }
  atualizar(id: number, req: NomeRequest): Observable<NomeResponse> { ... }
  remover(id: number): Observable<void> { ... }
}
```

**10. Component** — `frontend/src/app/components/nome/nome.component.ts` + `.html`
- `standalone: true`
- Estado via `signal<NomeResponse[]>([])`
- Template com `@if`, `@for` (NUNCA `*ngIf`, `*ngFor`)
- Formulário reativo com `ReactiveFormsModule`
- UI com Bootstrap 5

**11. Rota** — adicionar em `frontend/src/app/app.routes.ts`
```typescript
{
  path: 'nome',
  loadComponent: () => import('./components/nome/nome.component').then(m => m.NomeComponent),
  canActivate: [authGuard]
}
```

## Hard Rules (NUNCA violar)
- PROIBIDO: `*ngIf`, `*ngFor`, expor Entity em Controller, `System.out.println`, `console.log`
- OBRIGATÓRIO: Migration SQL por feature, `@Valid` nos controllers, `@Transactional` nas mutations, Signals no Angular
- Verificar o número da migration ANTES de gerar o arquivo

## Relatório Final
Ao terminar, liste todos os arquivos criados e qualquer passo manual necessário (ex: reiniciar backend para carregar migration).
