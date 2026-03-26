# GeriStreams — Análise Profunda: Service e Repository

> **Propósito deste documento:** Explicar em profundidade como as camadas `service/` e `repository/` foram construídas, quais padrões de design foram aplicados, como cada método funciona internamente e por que as decisões arquiteturais foram tomadas dessa forma.

---

## Índice

1. [Papel de cada camada](#1-papel-de-cada-camada)
2. [UsuarioRepository](#2-usuariorepository)
3. [AssinaturaRepository](#3-assinaturarepository)
4. [AuthService](#4-authservice)
5. [UsuarioService](#5-usuarioservice)
6. [AssinaturaService](#6-assinaturaservice)
7. [RelatorioService](#7-relatorioservice)
8. [AiService](#8-aiservice)
9. [Padrões e Decisões de Design](#9-padrões-e-decisões-de-design)

---

## 1. Papel de cada camada

```
Controller (HTTP)
     ↓ delega
  Service (BO — Business Object)    ← TODA regra de negócio fica aqui
     ↓ delega
  Repository (DAO)                  ← APENAS acesso a dados, sem lógica
     ↓ executa
  Banco de Dados (PostgreSQL)
```

**Princípio fundamental:** O Controller nunca acessa o Repository diretamente. Todo fluxo passa pelo Service. Isso garante que a lógica de negócio esteja centralizada, testável e independente do protocolo HTTP.

---

## 2. UsuarioRepository

```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### O que é `JpaRepository<Usuario, Long>`?

É uma interface genérica do Spring Data JPA. Ao declarar `extends JpaRepository<Usuario, Long>`:
- `Usuario` = tipo da entidade gerenciada
- `Long` = tipo da chave primária (o campo `@Id`)

O Spring gera automaticamente uma **implementação** dessa interface em tempo de execução (usando Proxy Java). Você nunca escreve a implementação manualmente.

**Métodos herdados automaticamente:**

| Método | SQL Equivalente | Uso no projeto |
|--------|----------------|----------------|
| `save(entity)` | `INSERT` ou `UPDATE` | Salvar/atualizar usuário |
| `findById(id)` | `SELECT WHERE id = ?` | Buscar usuário pelo ID |
| `findAll()` | `SELECT * FROM usuarios` | Listar todos (admin) |
| `delete(entity)` | `DELETE WHERE id = ?` | Remover usuário |
| `existsById(id)` | `SELECT COUNT(*) > 0 WHERE id = ?` | Verificar existência |
| `count()` | `SELECT COUNT(*) FROM usuarios` | Contar registros |

### `findByEmail(String email)` — Por que `Optional`?

```java
Optional<Usuario> findByEmail(String email);
```

**Spring Data Query Methods** — O Spring interpreta o nome do método:
- `find` = SELECT
- `By` = WHERE
- `Email` = campo `email` da entidade

**Por que `Optional<Usuario>` e não `Usuario`?**
- Um email pode não existir no banco (usuário não cadastrado)
- `Optional` força o código consumidor a tratar a ausência explicitamente
- Evita `NullPointerException` silencioso
- Padrão moderno em Java 8+

**Como é usado no projeto:**

```java
// Em AuthService.login()
Usuario usuario = usuarioRepository.findByEmail(dto.email()).orElseThrow();
//                                                            ↑ lança NoSuchElementException se vazio

// Em UsuarioService.getUsuarioAutenticado()
return usuarioRepository.findByEmail(email)
    .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
//   ↑ lança exceção customizada se vazio
```

### `existsByEmail(String email)` — Verificação antes de criar

```java
boolean existsByEmail(String email);
```

**Query gerada:** `SELECT COUNT(*) > 0 FROM usuarios WHERE email = ?`

**Por que usar `exists` em vez de `findBy + isPresent()`?**

```java
// Ineficiente (busca o objeto inteiro):
boolean existe = usuarioRepository.findByEmail(email).isPresent();

// Eficiente (apenas conta, sem carregar o objeto):
boolean existe = usuarioRepository.existsByEmail(email);
```

`existsByEmail` é mais performático porque o banco retorna apenas `true/false` sem trazer os dados do usuário (que não precisamos neste ponto).

**Uso em AuthService.registrar():**
```java
if (usuarioRepository.existsByEmail(dto.email())) {
    throw new IllegalArgumentException("E-mail já cadastrado.");
}
```

---

## 3. AssinaturaRepository

```java
public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {
    List<Assinatura> findByUsuarioId(Long usuarioId);
    List<Assinatura> findByUsuarioIdAndAtivoTrue(Long usuarioId);

    @Query("SELECT SUM(a.valor) FROM Assinatura a WHERE a.usuario.id = :usuarioId AND a.ativo = true")
    BigDecimal sumValorAtivoByUsuarioId(Long usuarioId);

    @Query("SELECT a.categoria, SUM(a.valor) FROM Assinatura a WHERE a.usuario.id = :usuarioId AND a.ativo = true GROUP BY a.categoria")
    List<Object[]> sumValorGroupedByCategoriaAndUsuarioId(Long usuarioId);

    @Query("SELECT a.usuario.id, SUM(a.valor) FROM Assinatura a WHERE a.ativo = true GROUP BY a.usuario.id ORDER BY SUM(a.valor) DESC")
    List<Object[]> rankingGastosPorUsuario();

    @Query("""
            SELECT a.nome, COUNT(a), SUM(a.valor), AVG(a.valor)
            FROM Assinatura a
            WHERE a.ativo = true
            GROUP BY a.nome
            ORDER BY SUM(a.valor) DESC
            """)
    List<Object[]> rankingServicos();
}
```

### `findByUsuarioId(Long usuarioId)` — Query Method automático

**Nome decodificado pelo Spring Data:**
- `find` = SELECT
- `By` = WHERE
- `UsuarioId` = campo `usuario.id` (navega pelo relacionamento `@ManyToOne`)

**SQL gerado automaticamente:**
```sql
SELECT * FROM assinaturas WHERE usuario_id = ?
```

O Spring entende que `Usuario` é um relacionamento e `Id` é a chave primária desse relacionamento. Nenhuma query manual necessária.

**Retorna:** Todas as assinaturas do usuário (ativas e inativas) — usado quando precisamos exibir todas.

### `findByUsuarioIdAndAtivoTrue(Long usuarioId)` — Query Method com múltiplas condições

**Nome decodificado:**
- `And` = AND no SQL
- `Ativo` = campo `ativo` (boolean)
- `True` = filtra apenas `ativo = true`

**SQL gerado:**
```sql
SELECT * FROM assinaturas WHERE usuario_id = ? AND ativo = true
```

**Diferença chave em relação ao anterior:** Este método filtra apenas assinaturas **ativas**. É o usado nos cálculos financeiros — assinaturas pausadas (`ativo = false`) não devem impactar o orçamento do usuário.

---

### `sumValorAtivoByUsuarioId` — Primeira `@Query` customizada

```java
@Query("SELECT SUM(a.valor) FROM Assinatura a WHERE a.usuario.id = :usuarioId AND a.ativo = true")
BigDecimal sumValorAtivoByUsuarioId(Long usuarioId);
```

**Por que `@Query` aqui e não Query Method?**

O Spring Data consegue gerar `findBy...`, `countBy...`, mas **não consegue gerar `SUM`** automaticamente. Para agregações (`SUM`, `AVG`, `COUNT`, `MAX`, `MIN`), precisamos de JPQL explícito.

**O que é JPQL?**
- Java Persistence Query Language
- Parecido com SQL, mas trabalha com **objetos Java**, não tabelas
- `FROM Assinatura a` = da classe Java `Assinatura`, não da tabela `assinaturas`
- `a.usuario.id` = navega pelo relacionamento `@ManyToOne` de `Assinatura` para `Usuario`
- O JPA traduz para SQL automaticamente (agnóstico de banco)

**`:usuarioId` — Named Parameter Binding:**
- `:nome` é um placeholder nomeado
- Seguro contra **SQL Injection** (o valor nunca é concatenado como string)
- Spring injeta o valor do parâmetro `Long usuarioId` automaticamente

**SQL equivalente gerado pelo JPA:**
```sql
SELECT SUM(valor) FROM assinaturas WHERE usuario_id = ? AND ativo = true
```

**Retorna `BigDecimal` (não `double` ou `float`):**
- `BigDecimal` é obrigatório para valores monetários
- `double` e `float` têm erros de precisão (ex: `0.1 + 0.2 = 0.30000000000000004`)
- `BigDecimal` garante precisão exata para operações financeiras

**Retorna `null` se não há assinaturas:**
- `SUM()` de nenhum registro retorna `NULL` no SQL
- O JPA mantém esse `null` no Java
- O Service deve verificar: `if (total == null) total = BigDecimal.ZERO;`

---

### `sumValorGroupedByCategoriaAndUsuarioId` — Agregação com GROUP BY

```java
@Query("SELECT a.categoria, SUM(a.valor) FROM Assinatura a WHERE a.usuario.id = :usuarioId AND a.ativo = true GROUP BY a.categoria")
List<Object[]> sumValorGroupedByCategoriaAndUsuarioId(Long usuarioId);
```

**O que faz:** Para cada categoria que o usuário possui, calcula o gasto total.

**SQL equivalente:**
```sql
SELECT categoria, SUM(valor)
FROM assinaturas
WHERE usuario_id = ? AND ativo = true
GROUP BY categoria
```

**Exemplo de resultado:**
```
categoria           | SUM(valor)
--------------------|-----------
STREAMING_VIDEO     | 250.00
STREAMING_MUSICA    |  80.00
SOFTWARE            |  55.50
```

**Por que `List<Object[]>`?**

Quando a query retorna múltiplas colunas que **não formam uma entidade completa**, o JPA retorna `Object[]` (array de objetos). Cada elemento da lista é uma linha da query:
- `row[0]` = `CategoriaAssinatura` (o enum)
- `row[1]` = `BigDecimal` (a soma)

**Como o Service processa:**
```java
assinaturaRepository.sumValorGroupedByCategoriaAndUsuarioId(usuarioId)
    .forEach(row -> gastosPorCategoria.put(
        ((CategoriaAssinatura) row[0]).name(), // "STREAMING_VIDEO"
        (BigDecimal) row[1]                    // 250.00
    ));
```

Por que `.name()`? O enum `CategoriaAssinatura` é enviado como `String` para o frontend. `.name()` retorna a representação em String (`"STREAMING_VIDEO"`).

---

### `rankingGastosPorUsuario` — Ranking por usuário (admin)

```java
@Query("SELECT a.usuario.id, SUM(a.valor) FROM Assinatura a WHERE a.ativo = true GROUP BY a.usuario.id ORDER BY SUM(a.valor) DESC")
List<Object[]> rankingGastosPorUsuario();
```

**SQL equivalente:**
```sql
SELECT usuario_id, SUM(valor)
FROM assinaturas
WHERE ativo = true
GROUP BY usuario_id
ORDER BY SUM(valor) DESC
```

**O que faz:** Lista usuários ordenados pelo gasto total em assinaturas ativas, do maior para o menor.

- `GROUP BY a.usuario.id` — agrupa por usuário
- `ORDER BY SUM(a.valor) DESC` — `DESC` = descendente, maior primeiro
- `row[0]` = Long (ID do usuário)
- `row[1]` = BigDecimal (gasto total)

---

### `rankingServicos` — Ranking de serviços com múltiplas agregações

```java
@Query("""
        SELECT a.nome, COUNT(a), SUM(a.valor), AVG(a.valor)
        FROM Assinatura a
        WHERE a.ativo = true
        GROUP BY a.nome
        ORDER BY SUM(a.valor) DESC
        """)
List<Object[]> rankingServicos();
```

**Text Blocks (`"""..."""`):** Java 15+. Permite strings multilinhas sem concatenação. Equivalente a uma string com `\n`, mas muito mais legível.

**SQL equivalente:**
```sql
SELECT nome, COUNT(*), SUM(valor), AVG(valor)
FROM assinaturas
WHERE ativo = true
GROUP BY nome
ORDER BY SUM(valor) DESC
```

**O que cada coluna retorna:**
- `row[0]` = String (`nome` do serviço, ex: "Netflix")
- `row[1]` = Long (`COUNT(a)` — quantos usuários assinam)
- `row[2]` = BigDecimal (`SUM(a.valor)` — gasto total por todos)
- `row[3]` = BigDecimal (`AVG(a.valor)` — média de valor)

**Como o Service converte:**
```java
return assinaturaRepository.rankingServicos().stream()
    .map(row -> new RankingAssinaturaDTO(
        (String) row[0],
        ((Number) row[1]).longValue(),             // COUNT pode ser Integer ou Long — cast seguro via Number
        row[2] instanceof BigDecimal bd ? bd       // Pattern matching Java 16+
            : BigDecimal.valueOf(((Number) row[2]).doubleValue()),
        row[3] instanceof BigDecimal bd ? bd
            : BigDecimal.valueOf(((Number) row[3]).doubleValue())
    ))
    .toList();
```

**Por que `instanceof BigDecimal bd ? bd : BigDecimal.valueOf(...)`?**

PostgreSQL pode retornar `AVG` como `Double` e `SUM` como `BigDecimal`, dependendo do tipo da coluna e da versão do driver. O código usa pattern matching com instanceof para lidar com ambos os casos de forma segura.

---

## 4. AuthService

```java
@Service
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
}
```

### Injeção de Dependências via Construtor

Todas as dependências são injetadas pelo construtor, não por `@Autowired` em campos. Vantagens:
1. **Testabilidade** — fácil criar instâncias com mocks nos testes
2. **Imutabilidade** — campos `final`, não podem ser alterados após construção
3. **Visibilidade clara** — quem lê o código vê todas as dependências no construtor
4. **Recomendado pelo próprio Spring** desde a versão 4.x

### `registrar(RegisterRequestDTO dto)`

```java
public JwtResponseDTO registrar(RegisterRequestDTO dto) {
    // 1. Validar unicidade de email
    if (usuarioRepository.existsByEmail(dto.email())) {
        throw new IllegalArgumentException("E-mail já cadastrado.");
    }

    // 2. Criar entidade
    Usuario usuario = new Usuario();
    usuario.setNome(dto.nome());
    usuario.setEmail(dto.email());

    // 3. Hash da senha (NUNCA salvar em texto puro)
    usuario.setSenha(passwordEncoder.encode(dto.senha()));
    usuario.setSalario(dto.salario());

    // 4. Persistir no banco (JPA gera ID automaticamente via IDENTITY)
    usuarioRepository.save(usuario);

    // 5. Carregar UserDetails (padrão Spring Security)
    UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());

    // 6. Gerar JWT
    String token = jwtUtil.generateToken(userDetails);

    // 7. Retornar DTO (nunca retornar a entidade diretamente)
    return new JwtResponseDTO(token, usuario.getEmail(), usuario.getRole().name());
}
```

**Por que `passwordEncoder.encode()`?**
- BCrypt é um algoritmo de hash **unidirecional** — impossível reverter para a senha original
- Inclui **salt** automático — mesmo valor gera hashes diferentes a cada execução
- Intencionalmente lento — dificulta ataques de força bruta

**Por que salvar e depois `loadUserByUsername`?**
- `save()` persiste e retorna a entidade com ID gerado
- `loadUserByUsername()` retorna `UserDetails` com as `GrantedAuthorities` (roles) carregadas do banco
- `generateToken()` precisa do `UserDetails` para inserir as roles no payload do JWT

### `login(LoginRequestDTO dto)`

```java
public JwtResponseDTO login(LoginRequestDTO dto) {
    // Valida credenciais — lança BadCredentialsException se errado
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(dto.email(), dto.senha())
    );

    // Credenciais OK — carregar dados para o token
    UserDetails userDetails = userDetailsService.loadUserByUsername(dto.email());
    String token = jwtUtil.generateToken(userDetails);

    // Buscar entidade completa para pegar a role
    Usuario usuario = usuarioRepository.findByEmail(dto.email()).orElseThrow();

    return new JwtResponseDTO(token, usuario.getEmail(), usuario.getRole().name());
}
```

**O que `authenticationManager.authenticate()` faz internamente?**
1. Chama `UserDetailsService.loadUserByUsername(email)` para buscar o usuário
2. Compara a senha fornecida com o hash armazenado usando `passwordEncoder.matches()`
3. Se as senhas coincidirem: autenticação bem-sucedida
4. Se não coincidirem: lança `BadCredentialsException`

O `GlobalExceptionHandler` captura essa exceção e retorna HTTP 401.

---

## 5. UsuarioService

### `getUsuarioAutenticado()` — O método mais importante do serviço

```java
public Usuario getUsuarioAutenticado() {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    return usuarioRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
}
```

**O que é o `SecurityContextHolder`?**

É o "cofre" central do Spring Security, armazenado como `ThreadLocal` (cada thread tem seu próprio contexto). Quando o `JwtAuthFilter` valida um token, ele armazena o usuário autenticado aqui:

```java
// Dentro do JwtAuthFilter:
SecurityContextHolder.getContext().setAuthentication(authToken);
```

Depois, em qualquer ponto da cadeia de chamadas (Service, etc.), o contexto pode ser recuperado:

```java
SecurityContextHolder.getContext().getAuthentication().getName()
// getName() retorna o "username" — que neste projeto é o email
```

**Por que esse método é crítico?**

Todos os outros métodos que precisam do usuário autenticado chamam este. Ele centraliza a lógica de recuperação do usuário, evitando duplicação.

**Por que `IllegalStateException` e não `IllegalArgumentException`?**
- `IllegalArgumentException` = problema com os dados de entrada do cliente
- `IllegalStateException` = estado inválido do sistema (não deveria acontecer se o JWT foi validado corretamente)

### `promoverParaAdmin(Long id)`

```java
@Transactional
public UsuarioResponseDTO promoverParaAdmin(Long id) {
    String emailAutenticado = SecurityContextHolder.getContext().getAuthentication().getName();

    Usuario alvo = usuarioRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

    // Regra 1: Não pode se auto-promover
    if (alvo.getEmail().equals(emailAutenticado)) {
        throw new IllegalArgumentException("Você não pode alterar sua própria role.");
    }

    // Regra 2: Não pode promover quem já é admin
    if (alvo.getRole() == Role.ADMIN) {
        throw new IllegalArgumentException("Usuário já é administrador.");
    }

    alvo.setRole(Role.ADMIN);
    return UsuarioResponseDTO.fromEntity(usuarioRepository.save(alvo));
}
```

**Por que a regra de auto-promoção existe?**
Se um administrador pudesse se promover, qualquer usuário que obtivesse acesso temporário a uma conta admin poderia escalar seus próprios privilégios de forma permanente.

**`@Transactional`:**
- Garante atomicidade: a mudança de role é confirmada (commit) somente se tudo der certo
- Se uma exceção for lançada após o `setRole()`, o banco reverte automaticamente (rollback)
- O Spring gerencia isso automaticamente via AOP (Aspect-Oriented Programming)

### `listarTodos()` — Stream API

```java
public List<UsuarioResponseDTO> listarTodos() {
    return usuarioRepository.findAll().stream()
            .map(UsuarioResponseDTO::fromEntity)
            .toList();
}
```

**Pipeline de Stream Java:**
1. `findAll()` — retorna `List<Usuario>` do banco
2. `.stream()` — converte para Stream (pipeline de operações lazy)
3. `.map(UsuarioResponseDTO::fromEntity)` — transforma cada `Usuario` em `UsuarioResponseDTO`
4. `.toList()` — coleta o Stream em uma `List` imutável (Java 16+)

**Method Reference `UsuarioResponseDTO::fromEntity`:**
Equivalente a `.map(u -> UsuarioResponseDTO.fromEntity(u))`. Mais conciso e legível.

---

## 6. AssinaturaService

Este é o Service mais complexo do projeto. Centraliza toda a lógica de negócio de assinaturas.

### `buscarPorIdDoUsuario(Long id)` — O guardião de segurança

```java
private Assinatura buscarPorIdDoUsuario(Long id) {
    Usuario usuario = usuarioService.getUsuarioAutenticado();
    return assinaturaRepository.findById(id)
            .filter(a -> a.getUsuario().getId().equals(usuario.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada."));
}
```

**Por que este método é CRÍTICO para segurança?**

Sem ele, qualquer usuário poderia manipular assinaturas de outros:
```
DELETE /api/subscriptions/999  ← 999 pode ser assinatura de outro usuário
```

Este método garante:
1. Busca a assinatura pelo ID
2. Verifica se o `usuario_id` da assinatura é igual ao ID do usuário autenticado
3. Se não for: a assinatura "some" (Optional fica vazio) e `orElseThrow()` lança exceção

**Por que retorna exceção genérica "não encontrada" em vez de "acesso negado"?**
Segurança por obscuridade — ao retornar "não encontrada" para ambos os casos (inexistente e pertencente a outro), evitamos vazar informação sobre a existência de assinaturas de outros usuários.

### `calcularResumoFinanceiro()` — A operação mais complexa

```java
public ResumoFinanceiroDTO calcularResumoFinanceiro() {
    Usuario usuario = usuarioService.getUsuarioAutenticado();
    Long usuarioId = usuario.getId();

    // 1. Listar assinaturas ativas
    List<AssinaturaResponseDTO> assinaturas = assinaturaRepository
            .findByUsuarioIdAndAtivoTrue(usuarioId)
            .stream()
            .map(AssinaturaResponseDTO::fromEntity)
            .toList();

    // 2. Calcular soma total
    BigDecimal total = assinaturaRepository.sumValorAtivoByUsuarioId(usuarioId);
    if (total == null) total = BigDecimal.ZERO; // SUM de conjunto vazio = null no SQL

    // 3. Calcular percentual
    BigDecimal salario = usuario.getSalario();
    BigDecimal percentual = salario.compareTo(BigDecimal.ZERO) > 0
            ? total.divide(salario, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

    // 4. Agrupar por categoria
    Map<String, BigDecimal> gastosPorCategoria = new LinkedHashMap<>();
    assinaturaRepository.sumValorGroupedByCategoriaAndUsuarioId(usuarioId)
            .forEach(row -> gastosPorCategoria.put(
                    ((CategoriaAssinatura) row[0]).name(),
                    (BigDecimal) row[1]
            ));

    return new ResumoFinanceiroDTO(salario, total, percentual, assinaturas, gastosPorCategoria);
}
```

**Análise de cada decisão:**

**`BigDecimal.ZERO` para total nulo:**
```java
if (total == null) total = BigDecimal.ZERO;
```
O JPQL `SUM()` retorna `null` quando não há linhas. Sem este tratamento, a divisão para calcular o percentual lançaria `NullPointerException`.

**Cálculo do percentual com proteção de divisão por zero:**
```java
BigDecimal percentual = salario.compareTo(BigDecimal.ZERO) > 0
    ? total.divide(salario, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
    : BigDecimal.ZERO;
```
- `compareTo(BigDecimal.ZERO) > 0` — nunca usa `==` ou `equals` para comparar `BigDecimal` com zero (pode ter scale diferente)
- `.divide(salario, 4, RoundingMode.HALF_UP)` — divisão com 4 casas decimais de precisão intermediária
- `.multiply(BigDecimal.valueOf(100))` — converte para percentual
- `HALF_UP` — arredondamento matemático padrão (5 arredonda para cima)

**`LinkedHashMap` para categorias:**
```java
Map<String, BigDecimal> gastosPorCategoria = new LinkedHashMap<>();
```
`LinkedHashMap` mantém a **ordem de inserção**. Importante para que o frontend receba as categorias sempre na mesma ordem, garantindo estabilidade nos gráficos.

### `alternarAtivo(Long id)` — Toggle pattern

```java
@Transactional
public AssinaturaResponseDTO alternarAtivo(Long id) {
    Assinatura assinatura = buscarPorIdDoUsuario(id);
    boolean novoStatus = !assinatura.getAtivo(); // Inverte o boolean
    assinatura.setAtivo(novoStatus);
    return AssinaturaResponseDTO.fromEntity(assinaturaRepository.save(assinatura));
}
```

**Por que `!assinatura.getAtivo()`?**
- Se estava `true` (ativo): `!true = false` (desativa)
- Se estava `false` (inativo): `!false = true` (ativa)

Um único endpoint, uma única operação, dois comportamentos — dependendo do estado atual. O frontend não precisa saber o estado atual para chamar o endpoint.

### `rankingServicos()` — Conversão de `Object[]` para DTO

```java
public List<RankingAssinaturaDTO> rankingServicos() {
    return assinaturaRepository.rankingServicos().stream()
            .map(row -> new RankingAssinaturaDTO(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    row[2] instanceof BigDecimal bd ? bd : BigDecimal.valueOf(((Number) row[2]).doubleValue()),
                    row[3] instanceof BigDecimal bd ? bd : BigDecimal.valueOf(((Number) row[3]).doubleValue())
            ))
            .toList();
}
```

**`((Number) row[1]).longValue()`:**
O `COUNT` pode ser retornado como `Integer` ou `Long` dependendo do banco. Converter para `Number` (superclasse comum) e então para `long` é a forma mais segura de lidar com isso.

**Pattern matching com `instanceof` (Java 16+):**
```java
row[2] instanceof BigDecimal bd ? bd : BigDecimal.valueOf(...)
```
Se `row[2]` for um `BigDecimal`, usa diretamente. Caso contrário (ex: `Double`), converte. A variável `bd` é criada inline — isso é o **Pattern Matching for instanceof** do Java moderno.

---

## 7. RelatorioService

```java
@Service
public class RelatorioService {
    private static final Color COR_PRIMARIA  = new Color(13, 110, 253);
    private static final Color COR_CINZA     = new Color(108, 117, 125);
    private static final Color COR_CABECALHO = new Color(248, 249, 250);

    public byte[] gerarRelatorioPdf() {
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        ResumoFinanceiroDTO resumo = assinaturaService.calcularResumoFinanceiro();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        adicionarCabecalho(doc, usuario);
        adicionarResumo(doc, resumo);
        adicionarTabelaAssinaturas(doc, resumo);
        adicionarGastosPorCategoria(doc, resumo);
        adicionarRodape(doc);

        doc.close();
        return out.toByteArray();
    }
}
```

**Por que `ByteArrayOutputStream` e não gravar em arquivo?**
- O PDF é gerado **inteiramente em memória** (RAM)
- `toByteArray()` retorna o conteúdo como `byte[]`
- O Controller envia esses bytes como resposta HTTP
- Não há necessidade de disco — não há arquivos temporários
- Mais seguro (sem preocupação com permissões de arquivo)
- Mais rápido (sem I/O de disco)

**Cores como constantes estáticas:**
```java
private static final Color COR_PRIMARIA = new Color(13, 110, 253); // Bootstrap Primary Blue
```
Definidas como `static final` porque são constantes compartilhadas por todos os métodos privados de formatação. Correspondem à paleta do Bootstrap 5 para consistência visual.

**Método principal delega para métodos privados:**
```java
adicionarCabecalho(doc, usuario);
adicionarResumo(doc, resumo);
adicionarTabelaAssinaturas(doc, resumo);
adicionarGastosPorCategoria(doc, resumo);
adicionarRodape(doc);
```
Cada método privado é responsável por uma seção do PDF — princípio de **Single Responsibility**. Fácil de manter e alterar independentemente.

---

## 8. AiService

```java
@Service
public class AiService {
    private static final String SYSTEM_PROMPT = """
        Você é um consultor financeiro pessoal especializado em assinaturas de streaming...
        """;

    private final RestClient anthropicRestClient;
    private final AssinaturaService assinaturaService;
    private final UsuarioService usuarioService;

    @Value("${anthropic.model}")
    private String model;
}
```

### `@Qualifier("anthropicRestClient")` — Por que é necessário?

```java
public AiService(@Qualifier("anthropicRestClient") RestClient anthropicRestClient, ...)
```

O Spring pode ter múltiplos beans do tipo `RestClient` (ex: um para a API Anthropic, outro para APIs internas). `@Qualifier` diz qual bean específico deve ser injetado — o nomeado `"anthropicRestClient"`, configurado em `RestClientConfig`.

### `@Value("${anthropic.model}")` — Injeção de propriedade

```java
@Value("${anthropic.model}")
private String model;
```

O Spring lê o valor de `application.properties`:
```properties
anthropic.model=claude-haiku-4-5
```
E injeta na variável `model`. Isso permite mudar o modelo sem recompilar o código — apenas alterar a configuração.

### `construirPrompt(String nome, ResumoFinanceiroDTO resumo)` — Prompt Engineering

```java
private String construirPrompt(String nome, ResumoFinanceiroDTO resumo) {
    StringBuilder sb = new StringBuilder();

    sb.append("Olá! Me chamo ").append(nome).append(".\n\n");
    sb.append("**Minha situação financeira atual com assinaturas:**\n");
    sb.append("- Salário mensal: R$ ").append(formatar(resumo.salario())).append("\n");
    // ... mais dados ...

    resumo.assinaturas().forEach(a ->
        sb.append("- ").append(a.nome())
          .append(" (").append(a.categoria()).append(")")
          .append(" — R$ ").append(formatar(a.valor())).append("/mês\n")
    );

    sb.append("Com base nessas informações, me dê 1 dicas práticas...");
    return sb.toString();
}
```

**Por que `StringBuilder` e não concatenação de Strings?**
- Concatenação de String cria um novo objeto a cada operação (`"a" + "b"` cria 2 Strings)
- `StringBuilder` acumula em buffer — 1 alocação, múltiplas operações
- Mais eficiente quando há muitas concatenações sequenciais

**Estrutura do prompt — Contexto + Tarefa:**
1. **Contexto:** dados do usuário (nome, salário, assinaturas, categorias)
2. **Tarefa:** instrução clara do que gerar
3. **System Prompt:** tom, idioma, formatação (enviado separadamente na API Anthropic)

### Chamada à API Anthropic

```java
AnthropicRequestDTO requestBody = new AnthropicRequestDTO(
    model,       // "claude-haiku-4-5"
    1024,        // max_tokens — limite de resposta
    SYSTEM_PROMPT,
    List.of(new AnthropicMessageDTO("user", prompt)),
    false        // stream = false (resposta completa, não streaming)
);

AnthropicResponseDTO response = anthropicRestClient
    .post()
    .uri("/v1/messages")
    .body(requestBody)
    .retrieve()
    .body(AnthropicResponseDTO.class);
```

**`RestClient` (Spring 6.1+):**
- API fluente (método encadeado) para requisições HTTP
- Alternativa moderna ao `RestTemplate`
- `.body(AnthropicResponseDTO.class)` — deserializa automaticamente JSON para DTO

**`response.extractText()`:**
O `AnthropicResponseDTO` tem um método auxiliar que extrai o texto do array de `content_blocks` retornado pela API:
```java
// Estrutura da resposta Anthropic:
// { "content": [{ "type": "text", "text": "Sua análise..." }] }
```

---

## 9. Padrões e Decisões de Design

### Padrão Repository (DAO)

O Repository abstrai o mecanismo de persistência. O Service não sabe se os dados vêm de PostgreSQL, MySQL ou de um arquivo — só sabe que pode chamar `findByUsuarioId()`.

### Padrão Service (Business Object)

Toda validação, cálculo e regra de negócio está no Service. Isso torna:
- **Controllers simples** — apenas roteiam requisições
- **Testável** — Services podem ser testados sem HTTP (unitários)
- **Reutilizável** — dois Controllers podem chamar o mesmo Service

### Isolamento por usuário como regra central

Toda operação do usuário comum passa pelo `getUsuarioAutenticado()`. O Service **nunca aceita `userId` como parâmetro de entrada** para operações do próprio usuário — sempre extrai do contexto de segurança. Isso elimina a possibilidade de parameter pollution (usuário fornecendo ID de outro usuário).

### `@Transactional` somente onde necessário

Apenas métodos que modificam dados têm `@Transactional`:
- `criar`, `atualizar`, `remover`, `alternarAtivo` — escrevem no banco
- `listar`, `calcularResumoFinanceiro` — apenas leitura, não precisam de transação

Transações desnecessárias consomem recursos do pool de conexões.

### Nunca expor Entity no Controller

```java
// ERRADO (expõe entidade JPA com senha, lazy collections, etc):
return usuario; // Entidade com campo senha!

// CORRETO (DTO controlado, sem campo senha):
return UsuarioResponseDTO.fromEntity(usuario);
```

`fromEntity()` é um método de fábrica estático no DTO que controla exatamente quais campos são expostos.

### Logging com SLF4J

Todos os Services usam `Logger`:
```java
private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
logger.info("Usuário registrado: {}", email);
logger.warn("E-mail duplicado: {}", email);
logger.error("Erro inesperado", ex);
```
- `{}` é um placeholder (não concatena String — mais eficiente)
- `info` = eventos normais de negócio
- `warn` = situações esperadas mas anômalas
- `error` = erros inesperados que requerem atenção
- **Nunca `System.out.println`** (regra do projeto)

### BigDecimal para valores monetários

```java
private BigDecimal valor; // ✓ Correto para dinheiro
private double valor;     // ✗ Errado — perde precisão
```

`double 0.1 + 0.2 = 0.30000000000000004` (erro de ponto flutuante).
`BigDecimal` garante precisão decimal exata para operações financeiras.
