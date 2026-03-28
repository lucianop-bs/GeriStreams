---
globs: backend/src/main/java/com/projeto/**/*.java
---

# Regras Java — Spring Boot 3 / GeriStreams

## Logger (Slf4j)
```java
// CORRETO — em toda classe de Service e Controller
private static final Logger logger = LoggerFactory.getLogger(NomeDaClasse.class);
logger.info("Operação realizada: {}", dado);
logger.error("Erro ao processar: {}", erro.getMessage(), erro);

// PROIBIDO
System.out.println("debug");
e.printStackTrace();
```

## Injeção de Dependência
```java
// CORRETO — constructor injection (ou @RequiredArgsConstructor do Lombok)
@Service
@RequiredArgsConstructor
public class NomeService {
    private final NomeRepository nomeRepository;
    private final UsuarioService usuarioService;
}

// PROIBIDO — field injection
@Autowired
private NomeRepository nomeRepository;
```

## DTOs

### Request DTO (Java record + validações)
```java
public record NomeRequestDTO(
    @NotBlank(message = "Nome é obrigatório") String nome,
    @NotNull(message = "Valor é obrigatório") @Positive BigDecimal valor,
    @NotNull CategoriaAssinatura categoria
) {}
```

### Response DTO (classe com factory method)
```java
public class NomeResponseDTO {
    private Long id;
    private String nome;
    private BigDecimal valor;
    private LocalDateTime createdAt;

    public static NomeResponseDTO fromEntity(NomeEntity entity) {
        NomeResponseDTO dto = new NomeResponseDTO();
        dto.id = entity.getId();
        dto.nome = entity.getNome();
        dto.valor = entity.getValor();
        dto.createdAt = entity.getCreatedAt();
        return dto;
    }
    // getters omitidos com Lombok @Getter
}
```

## Service
```java
@Service
@RequiredArgsConstructor
public class NomeService {
    private static final Logger logger = LoggerFactory.getLogger(NomeService.class);
    private final NomeRepository nomeRepository;

    // OBRIGATÓRIO: @Transactional em métodos que mutam dados
    @Transactional
    public NomeResponseDTO criar(NomeRequestDTO dto, Usuario usuario) {
        NomeEntity entity = new NomeEntity();
        entity.setNome(dto.nome());
        entity.setValor(dto.valor());
        entity.setUsuario(usuario);
        NomeEntity salvo = nomeRepository.save(entity);
        logger.info("Entidade criada: id={}", salvo.getId());
        return NomeResponseDTO.fromEntity(salvo);
    }
}
```

## Controller
```java
@RestController
@RequestMapping("/api/recurso")
@Tag(name = "Recurso", description = "Endpoints para gerenciar recursos")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class NomeController {
    private final NomeService nomeService;

    @PostMapping
    @Operation(summary = "Cria um novo recurso")
    public ResponseEntity<NomeResponseDTO> criar(
            @Valid @RequestBody NomeRequestDTO dto,  // @Valid OBRIGATÓRIO
            @AuthenticationPrincipal UserDetails userDetails) {
        // Nunca retornar Entity — sempre DTO
        return ResponseEntity.status(201).body(nomeService.criar(dto, ...));
    }
}
```

## Entity JPA
```java
@Entity
@Table(name = "nomes")
@Getter @Setter @NoArgsConstructor
public class NomeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategoriaAssinatura categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

## Flyway Migration
- Toda alteração de schema DEVE ter uma migration correspondente
- Arquivo: `backend/src/main/resources/db/migration/VN__descricao_em_snake_case.sql`
- Verificar o número da última migration antes de criar uma nova
- `ddl-auto=validate` — o schema SQL DEVE corresponder exatamente às entidades JPA
