# GeriStreams — Service Layer: Tudo Por Dentro e Por Fora

> Documento de máxima profundidade sobre a camada de Service. Cobre Java Streams, lambdas, method references, Optional, BigDecimal, mappers (fromEntity), Records, @Transactional, SecurityContextHolder, e a integração completa com IA (Anthropic Claude). Cada linha de código explicada.

---

## Índice

1. [A Missão do Service](#1-a-missão-do-service)
2. [Records Java — O que são os DTOs](#2-records-java--o-que-são-os-dtos)
3. [O Mapper Pattern — fromEntity()](#3-o-mapper-pattern--fromentity)
4. [Java Streams — A Espinha Dorsal](#4-java-streams--a-espinha-dorsal)
5. [Lambdas e Method References](#5-lambdas-e-method-references)
6. [Optional — Tratando a Ausência de Valor](#6-optional--tratando-a-ausência-de-valor)
7. [BigDecimal — Precisão Financeira](#7-bigdecimal--precisão-financeira)
8. [UsuarioService — Linha a Linha](#8-usuarioservice--linha-a-linha)
9. [AuthService — Linha a Linha](#9-authservice--linha-a-linha)
10. [AssinaturaService — Linha a Linha](#10-assinaturaservice--linha-a-linha)
11. [@Transactional — AOP Por Baixo dos Panos](#11-transactional--aop-por-baixo-dos-panos)
12. [SecurityContextHolder — ThreadLocal Explicado](#12-securitycontextholder--threadlocal-explicado)
13. [RelatorioService — Linha a Linha](#13-relatorioservice--linha-a-linha)
14. [AiService — Linha a Linha e Integração IA](#14-aiservice--linha-a-linha-e-integração-ia)
15. [Jackson — Serialização e Deserialização JSON](#15-jackson--serialização-e-deserialização-json)
16. [O Caminho Completo de uma Requisição](#16-o-caminho-completo-de-uma-requisição)

---

## 1. A Missão do Service

```
CONTROLLER
    recebe a requisição HTTP
    valida o DTO de entrada (@Valid)
    chama o Service
    retorna o ResponseEntity
    NÃO contém lógica

SERVICE ← VOCÊ ESTÁ AQUI
    contém TODA a regra de negócio
    valida regras de domínio (email único, não auto-promoção)
    faz cálculos (percentual financeiro)
    orquestra chamadas ao Repository
    converte Entity → DTO (mapper)
    gerencia transações (@Transactional)
    acessa o contexto de segurança (SecurityContextHolder)

REPOSITORY
    executa SQL no banco
    NÃO contém lógica de negócio
    retorna entidades JPA ou tipos primitivos
```

**Regra fundamental:** O Controller nunca acessa o Repository. O Repository nunca acessa o Service. A dependência é sempre de cima para baixo.

---

## 2. Records Java — O que são os DTOs

Antes de entender o mapper, é preciso entender o que é um `record`.

**Record** é uma classe especial do Java 16+ que o compilador gera automaticamente com:
- Campos privados e finais
- Construtor completo (com todos os campos)
- Getters no formato `campo()` (não `getCampo()`)
- `equals()`, `hashCode()` e `toString()` automáticos
- **Imutável**: depois de criado, nenhum campo pode ser alterado

```java
// Você escreve isso:
public record AssinaturaResponseDTO(
    Long id,
    String nome,
    BigDecimal valor,
    String categoria,
    Boolean ativo,
    LocalDateTime createdAt
) {}

// O compilador gera equivalente a isso:
public final class AssinaturaResponseDTO {
    private final Long id;
    private final String nome;
    private final BigDecimal valor;
    private final String categoria;
    private final Boolean ativo;
    private final LocalDateTime createdAt;

    // Construtor gerado:
    public AssinaturaResponseDTO(Long id, String nome, BigDecimal valor,
                                  String categoria, Boolean ativo, LocalDateTime createdAt) {
        this.id = id;
        this.nome = nome;
        this.valor = valor;
        this.categoria = categoria;
        this.ativo = ativo;
        this.createdAt = createdAt;
    }

    // Getters gerados (sem "get"):
    public Long id()              { return id; }
    public String nome()          { return nome; }
    public BigDecimal valor()     { return valor; }
    public String categoria()     { return categoria; }
    public Boolean ativo()        { return ativo; }
    public LocalDateTime createdAt() { return createdAt; }

    // equals(), hashCode(), toString() também gerados
}
```

**Por que Records para DTOs?**
- Imutáveis: um DTO criado não muda → segurança
- Compactos: menos código para escrever
- Semântica correta: DTO é um "pacote de dados", não um objeto com comportamento

---

## 3. O Mapper Pattern — fromEntity()

O mapper é o responsável por **converter uma Entidade JPA em um DTO**. No projeto, ele é um método estático dentro do próprio DTO chamado `fromEntity()`.

### Por que converter Entity → DTO?

```
Entity (Assinatura) tem:             DTO (AssinaturaResponseDTO) tem:
  - id (Long)              →            id (Long)          ✓
  - nome (String)          →            nome (String)      ✓
  - valor (BigDecimal)     →            valor (BigDecimal) ✓
  - categoria (enum)       →            categoria (String) ✓ ← CONVERTIDO
  - ativo (Boolean)        →            ativo (Boolean)    ✓
  - usuario (Usuario)      →            [NÃO EXPOSTO]      ✗ ← REMOVIDO
  - createdAt              →            createdAt          ✓

Entity (Usuario) tem:                DTO (UsuarioResponseDTO) tem:
  - id                     →            id                 ✓
  - nome                   →            nome               ✓
  - email                  →            email              ✓
  - senha (hash BCrypt)    →            [NÃO EXPOSTO]      ✗ ← SEGURANÇA!
  - salario                →            salario            ✓
  - role (enum)            →            role (String)      ✓ ← CONVERTIDO
  - createdAt              →            createdAt          ✓
  - assinaturas (List)     →            [NÃO EXPOSTO]      ✗ ← EVITA LAZY EXCEPTION
```

### Como `fromEntity()` funciona linha a linha

```java
// AssinaturaResponseDTO.java
public record AssinaturaResponseDTO(
    Long id, String nome, BigDecimal valor,
    String categoria, Boolean ativo, LocalDateTime createdAt
) {
    // Método estático: pode ser chamado sem instância
    // Recebe a Entity "crua" do banco
    public static AssinaturaResponseDTO fromEntity(Assinatura assinatura) {

        return new AssinaturaResponseDTO(
            assinatura.getId(),            // Long → Long (direto)
            assinatura.getNome(),           // String → String (direto)
            assinatura.getValor(),          // BigDecimal → BigDecimal (direto)
            assinatura.getCategoria().name(), // ← CONVERSÃO IMPORTANTE!
            // getCategoria() retorna: CategoriaAssinatura.STREAMING_VIDEO (enum)
            // .name()          retorna: "STREAMING_VIDEO" (String)
            // O frontend não conhece o enum Java — precisa da String
            assinatura.getAtivo(),          // Boolean → Boolean (direto)
            assinatura.getCreatedAt()       // LocalDateTime → LocalDateTime (direto)
            // Nota: assinatura.getUsuario() NÃO é incluído aqui
            // Motivo 1: frontend não precisa
            // Motivo 2: getUsuario() pode lançar LazyInitializationException
            //           (FetchType.LAZY — carrega do banco só quando acessado)
        );
    }
}
```

**O que é `LazyInitializationException`?**

```
Assinatura tem: @ManyToOne(fetch = FetchType.LAZY)
                private Usuario usuario;

Quando você busca uma Assinatura do banco:
  SELECT * FROM assinaturas WHERE id = 5
  → usuario é um PROXY (objeto fake do Hibernate)
  → Não foi buscado do banco ainda (lazy = preguiçoso)

Se você chamar assinatura.getUsuario().getNome() FORA de uma transação:
  → Hibernate tenta fazer SELECT * FROM usuarios WHERE id = 3
  → Mas a sessão JPA já foi fechada!
  → LazyInitializationException! ← Erro clássico de JPA

Solução: o mapper NÃO acessa assinatura.getUsuario()
         → Nunca causa o problema
```

### O mapper de Usuario

```java
// UsuarioResponseDTO.java
public static UsuarioResponseDTO fromEntity(Usuario usuario) {
    return new UsuarioResponseDTO(
        usuario.getId(),
        usuario.getNome(),
        usuario.getEmail(),
        usuario.getSalario(),
        usuario.getRole().name(), // Role.USER → "USER" | Role.ADMIN → "ADMIN"
        usuario.getCreatedAt()
        // usuario.getSenha() NÃO incluído ← NUNCA envie senha ao frontend!
        // usuario.getAssinaturas() NÃO incluído ← LazyInitializationException + desnecessário
    );
}
```

---

## 4. Java Streams — A Espinha Dorsal

Stream API (Java 8+) é a forma funcional de processar coleções de dados. É usado em quase todo método dos Services.

### O que é um Stream?

```
Lista normal (Collection):
  List<Assinatura> = [Assinatura1, Assinatura2, Assinatura3]
  → Você itera com for, acumula em outra lista, etc.
  → Código imperativo: "faça isso, depois isso, depois isso"

Stream:
  list.stream() = [ Assinatura1 ──▶ Assinatura2 ──▶ Assinatura3 ]
  → Um fluxo de dados com pipeline de transformações
  → Código declarativo: "quero isso desse jeito"
  → LAZY: as operações intermediárias só executam quando há uma
           operação terminal (.toList(), .findFirst(), .count()...)
```

### Anatomia de um Stream

```
┌─────────────────────────────────────────────────────────────────┐
│                    PIPELINE DE STREAM                            │
│                                                                  │
│  list.stream()          ← FONTE (cria o stream)                 │
│        │                                                         │
│        ▼                                                         │
│  .filter(predicate)     ← INTERMEDIÁRIO (filtra elementos)      │
│        │                                                         │
│        ▼                                                         │
│  .map(function)         ← INTERMEDIÁRIO (transforma elementos)  │
│        │                                                         │
│        ▼                                                         │
│  .sorted(comparator)    ← INTERMEDIÁRIO (ordena)                │
│        │                                                         │
│        ▼                                                         │
│  .toList()              ← TERMINAL (coleta resultado)           │
│  (ou .findFirst(), .count(), .forEach(), .collect()...)         │
└─────────────────────────────────────────────────────────────────┘
```

### O que é "LAZY"?

```java
// Este código NÃO executa filter ou map ainda:
Stream<AssinaturaResponseDTO> stream = lista.stream()
    .filter(a -> a.getAtivo())
    .map(AssinaturaResponseDTO::fromEntity);

// Só executa quando você chama a operação TERMINAL:
List<AssinaturaResponseDTO> resultado = stream.toList(); // ← AGORA executa tudo
```

Isso permite ao Java otimizar — por exemplo, com `.findFirst()`, ele para no primeiro elemento que passar pelo filtro, sem processar o resto.

### Streams usados no projeto — cada um explicado

#### Stream 1: `listar()` em AssinaturaService

```java
public List<AssinaturaResponseDTO> listar() {
    Usuario usuario = usuarioService.getUsuarioAutenticado();

    return assinaturaRepository.findByUsuarioId(usuario.getId())
    //     └─ Retorna: List<Assinatura>
    //        [Assinatura(id=1,nome="Netflix",...),
    //         Assinatura(id=2,nome="Spotify",...),
    //         Assinatura(id=3,nome="Disney+",...)]

            .stream()
    //     └─ Cria um Stream<Assinatura>
    //        Nada foi executado ainda

            .map(AssinaturaResponseDTO::fromEntity)
    //     └─ Para CADA Assinatura no stream:
    //        chama AssinaturaResponseDTO.fromEntity(assinatura)
    //        transforma Assinatura → AssinaturaResponseDTO
    //        Stream agora é Stream<AssinaturaResponseDTO>

            .toList();
    //     └─ TERMINAL: coleta todos os elementos
    //        Retorna List<AssinaturaResponseDTO> imutável
    //        [AssinaturaResponseDTO(id=1,nome="Netflix",...),
    //         AssinaturaResponseDTO(id=2,nome="Spotify",...),
    //         AssinaturaResponseDTO(id=3,nome="Disney+",...)]
}
```

**Passo a passo visual:**
```
List<Assinatura>              Stream pipeline              List<AssinaturaResponseDTO>
┌─────────────┐               ┌──────────────┐             ┌──────────────────────┐
│ Assinatura  │ ──.stream()──▶│ .map(        │──.toList()─▶│ AssinaturaResponseDTO│
│ id=1        │               │  fromEntity) │             │ id=1                 │
│ nome=Netflix│               │             │             │ nome=Netflix         │
│ usuario=obj │               │  remove obj  │             │ (sem usuario!)       │
│ categoria=  │               │  usuario     │             │ categoria="STRM..."  │
│  ENUM       │               │  converte    │             │ (String, não enum)   │
└─────────────┘               │  enum→String│             └──────────────────────┘
┌─────────────┐               │             │             ┌──────────────────────┐
│ Assinatura  │──────────────▶│             │────────────▶│ AssinaturaResponseDTO│
│ id=2        │               │             │             │ id=2                 │
│ nome=Spotify│               └──────────────┘             │ nome=Spotify         │
└─────────────┘                                            └──────────────────────┘
```

#### Stream 2: `listarTodos()` em UsuarioService

```java
public List<UsuarioResponseDTO> listarTodos() {
    return usuarioRepository.findAll()
    //     └─ SELECT * FROM usuarios
    //        Retorna List<Usuario> com TODOS os usuários

            .stream()
            .map(UsuarioResponseDTO::fromEntity)
    //     └─ Cada Usuario → UsuarioResponseDTO
    //        REMOVE a senha de cada um automaticamente
            .toList();
}
```

#### Stream 3: `calcularResumoFinanceiro()` em AssinaturaService

```java
// Parte do método:
List<AssinaturaResponseDTO> assinaturas = assinaturaRepository
    .findByUsuarioIdAndAtivoTrue(usuarioId)
    // └─ SELECT * FROM assinaturas
    //    WHERE usuario_id = 3 AND ativo = true
    //    Retorna: apenas assinaturas ATIVAS
    .stream()
    .map(AssinaturaResponseDTO::fromEntity)
    // └─ Converte cada Assinatura (Entity) → AssinaturaResponseDTO
    .toList();
```

#### Stream 4: `rankingServicos()` em AssinaturaService

```java
public List<RankingAssinaturaDTO> rankingServicos() {
    return assinaturaRepository.rankingServicos()
    //     └─ Retorna: List<Object[]>
    //        Cada Object[] é uma linha da query JPQL:
    //        [0] = "Netflix" (String)
    //        [1] = 250L (Long — COUNT)
    //        [2] = 12500.00 (BigDecimal — SUM)
    //        [3] = 50.00 (BigDecimal — AVG)

            .stream()

            .map(row -> new RankingAssinaturaDTO(
    //          └─ Lambda: recebe row (Object[]), retorna RankingAssinaturaDTO
    //             Para CADA linha do ranking:

                (String) row[0],
    //          └─ Cast: Object → String
    //             row[0] é o nome do serviço ("Netflix")
    //             Sempre é String porque a.nome é VARCHAR no banco

                ((Number) row[1]).longValue(),
    //          └─ Primeiro cast: Object → Number
    //             Por quê Number e não Long?
    //             COUNT pode retornar Integer ou Long dependendo do banco/driver
    //             Number é superclasse de ambos → seguro
    //             .longValue() converte para long (primitivo) e autobox para Long

                row[2] instanceof BigDecimal bd ? bd
                    : BigDecimal.valueOf(((Number) row[2]).doubleValue()),
    //          └─ Pattern Matching (Java 16+):
    //             "Se row[2] É um BigDecimal, guarda em 'bd' e usa ele"
    //             "Se NÃO for BigDecimal (ex: Double), converte via Number"
    //             SUM em PostgreSQL com NUMERIC retorna BigDecimal ✓
    //             Mas drivers diferentes podem retornar Double → fallback

                row[3] instanceof BigDecimal bd ? bd
                    : BigDecimal.valueOf(((Number) row[3]).doubleValue())
    //          └─ Mesmo raciocínio para AVG
            ))

            .toList();
}
```

#### Stream 5: `extractText()` em AnthropicResponseDTO (IA)

```java
public String extractText() {
    if (content == null) return "";

    return content.stream()
    //     └─ content é List<AnthropicContentBlockDTO>
    //        A API Anthropic retorna um array de blocos:
    //        [{ "type": "text", "text": "Sua análise..." }]
    //        (pode ter outros tipos como "tool_use" em respostas complexas)

            .filter(b -> "text".equals(b.type()))
    //     └─ Filtra: apenas blocos do tipo "text"
    //        b.type() retorna o campo type do record
    //        "text".equals(b.type()) ← boa prática: constante.equals(variável)
    //        Evita NullPointerException se b.type() for null

            .map(AnthropicContentBlockDTO::text)
    //     └─ Extrai o campo text de cada bloco
    //        AnthropicContentBlockDTO::text é method reference
    //        Equivale a: .map(b -> b.text())
    //        Transforma: Stream<AnthropicContentBlockDTO> → Stream<String>

            .findFirst()
    //     └─ TERMINAL: pega o PRIMEIRO elemento do stream
    //        Retorna: Optional<String>
    //        Se o stream estiver vazio: Optional.empty()
    //        Se tiver elementos: Optional.of("Sua análise...")

            .orElse("");
    //     └─ Se Optional vazio: retorna string vazia ""
    //        Se Optional presente: retorna o texto da IA
}
```

---

## 5. Lambdas e Method References

### O que é uma Lambda?

Lambda é uma **função anônima** — uma função sem nome que pode ser passada como argumento.

```java
// Forma longa (antes de Java 8):
new Predicate<Assinatura>() {
    @Override
    public boolean test(Assinatura a) {
        return a.getAtivo();
    }
}

// Forma lambda (Java 8+):
a -> a.getAtivo()

// Lê-se: "dado um 'a', retorna a.getAtivo()"
//         ↑ parâmetro   ↑ corpo (o que retorna)
```

### Lambdas no projeto

```java
// Em buscarPorIdDoUsuario():
.filter(a -> a.getUsuario().getId().equals(usuario.getId()))
// "dado uma assinatura 'a', retorna true se o ID do usuário bate"

// Em rankingServicos():
.map(row -> new RankingAssinaturaDTO(...))
// "dado um array 'row', cria e retorna um RankingAssinaturaDTO"

// Em calcularResumoFinanceiro():
assinaturaRepository.sumValorGroupedByCategoriaAndUsuarioId(usuarioId)
    .forEach(row -> gastosPorCategoria.put(
        ((CategoriaAssinatura) row[0]).name(),
        (BigDecimal) row[1]
    ));
// "para cada 'row', adiciona no mapa"
// forEach recebe um Consumer<Object[]> — consome cada elemento sem retornar nada

// Em construirPrompt() (AiService):
resumo.assinaturas().forEach(a ->
    sb.append("- ").append(a.nome())
      .append(" (").append(a.categoria()).append(")")
      .append(" — R$ ").append(formatar(a.valor())).append("/mês\n")
);
// "para cada assinatura 'a', adiciona uma linha no StringBuilder"
```

### O que é Method Reference (`::`)?

Method Reference é um atalho para lambdas que apenas **chamam um método existente**.

```java
// Lambda:
.map(assinatura -> AssinaturaResponseDTO.fromEntity(assinatura))

// Method Reference equivalente:
.map(AssinaturaResponseDTO::fromEntity)

// Lê-se: "chame o método fromEntity da classe AssinaturaResponseDTO"
//         passando o elemento do stream como argumento
```

**Tipos de Method Reference:**

```java
// 1. Método estático de classe:
.map(AssinaturaResponseDTO::fromEntity)
// Equivale: .map(a -> AssinaturaResponseDTO.fromEntity(a))

// 2. Método de instância via classe:
.map(AnthropicContentBlockDTO::text)
// Equivale: .map(b -> b.text())
// O elemento do stream é o "objeto" no qual o método é chamado

// 3. Método de instância de objeto específico:
usuarios.stream().map(mapper::toDTO)
// Equivale: .map(u -> mapper.toDTO(u))
// 'mapper' é uma variável — sempre usa a mesma instância

// 4. Construtor:
.map(String::new)
// Equivale: .map(s -> new String(s))
```

**No projeto, os method references usados:**

| Method Reference | Equivalente Lambda | Onde |
|---|---|---|
| `AssinaturaResponseDTO::fromEntity` | `a -> AssinaturaResponseDTO.fromEntity(a)` | `AssinaturaService.listar()` |
| `UsuarioResponseDTO::fromEntity` | `u -> UsuarioResponseDTO.fromEntity(u)` | `UsuarioService.listarTodos()` |
| `AnthropicContentBlockDTO::text` | `b -> b.text()` | `AnthropicResponseDTO.extractText()` |

---

## 6. Optional — Tratando a Ausência de Valor

`Optional<T>` é um contêiner que pode ou não conter um valor. É a solução Java para evitar `NullPointerException`.

### Como funciona internamente

```java
// Optional é como uma caixa:
Optional<Usuario> optional = usuarioRepository.findByEmail("joao@email.com");

// Caso 1 — Caixa COM valor:
// Optional { value = Usuario(id=3, email="joao@email.com", ...) }

// Caso 2 — Caixa VAZIA:
// Optional { value = null } ← internamente, mas você nunca acessa direto

// Verificar se tem valor:
optional.isPresent()  // true se tem valor
optional.isEmpty()    // true se está vazio (Java 11+)

// Pegar o valor:
optional.get()                    // ← PERIGOSO: NoSuchElementException se vazio
optional.orElse(valorPadrao)      // retorna valor ou o padrão
optional.orElseThrow(...)         // retorna valor ou lança exceção
optional.orElseGet(() -> calc())  // retorna valor ou chama função
optional.ifPresent(u -> ...)      // executa apenas se tiver valor

// Transformar o valor (SEM sair do Optional):
optional.map(u -> u.getNome())    // Optional<String>
optional.filter(u -> u.isAdmin()) // Optional<Usuario> ou Optional.empty()
```

### Optional no projeto — cada uso

```java
// 1. UsuarioService.buscarPorId()
return usuarioRepository.findById(id)      // Optional<Usuario>
    .map(UsuarioResponseDTO::fromEntity)    // Se presente: Optional<UsuarioResponseDTO>
    .orElseThrow(() -> {                    // Se vazio: lança exceção
        logger.warn("Usuário não encontrado com ID: {}", id);
        return new IllegalArgumentException("Usuário não encontrado.");
    });
// Fluxo: findById retorna Optional
//        .map() → se tem usuario, converte para DTO (ainda Optional)
//        .orElseThrow() → se chegou até aqui sem valor: exceção

// 2. UsuarioService.getUsuarioAutenticado()
return usuarioRepository.findByEmail(email)
    .orElseThrow(() -> {
        logger.error("Usuário autenticado não encontrado no banco: {}", email);
        return new IllegalStateException("Usuário autenticado não encontrado.");
    });
// Fluxo: busca por email → se não encontrar: erro de estado (anomalia do sistema)

// 3. AssinaturaService.buscarPorIdDoUsuario()
return assinaturaRepository.findById(id)
    .filter(a -> a.getUsuario().getId().equals(usuario.getId()))
    // ↑ .filter() no Optional:
    //   Se o Optional TEM valor E o filtro passa → mantém o Optional com valor
    //   Se o Optional TEM valor MAS filtro NÃO passa → Optional.empty()
    //   Se o Optional JÁ está vazio → continua vazio
    .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada."));
// Fluxo:
//   findById(5) → Optional<Assinatura>
//   .filter() → verifica se pertence ao usuário
//   Se pertencer: Optional<Assinatura> com valor
//   Se não pertencer: Optional.empty()
//   .orElseThrow() → se vazio: lança exceção "não encontrada"

// 4. AuthService.login()
Usuario usuario = usuarioRepository.findByEmail(dto.email()).orElseThrow();
// .orElseThrow() sem argumento → lança NoSuchElementException
// Mas aqui é seguro porque authenticationManager.authenticate() já validou
// Se email não existisse, o authenticate() já teria lançado BadCredentialsException

// 5. extractText() em AnthropicResponseDTO
.findFirst()         // Optional<String>
.orElse("")          // Se vazio: retorna string vazia
```

---

## 7. BigDecimal — Precisão Financeira

`BigDecimal` representa números decimais com precisão arbitrária. **Obrigatório para dinheiro.**

### Por que double/float falham com dinheiro

```java
double a = 0.1;
double b = 0.2;
System.out.println(a + b); // 0.30000000000000004 ← ERRADO!

BigDecimal a = new BigDecimal("0.1");
BigDecimal b = new BigDecimal("0.2");
System.out.println(a.add(b)); // 0.3 ← CORRETO
```

### Operações BigDecimal no projeto

```java
// Em calcularResumoFinanceiro():

// SOMA via banco (mais eficiente que somar no Java):
BigDecimal total = assinaturaRepository.sumValorAtivoByUsuarioId(usuarioId);
// SELECT SUM(valor) FROM assinaturas WHERE usuario_id = ? AND ativo = true
// Retorna: null se não há assinaturas (SQL SUM de zero rows = NULL)

// Tratando null:
if (total == null) total = BigDecimal.ZERO;
// BigDecimal.ZERO é uma constante: new BigDecimal("0")
// Equivale a: total = new BigDecimal("0")

BigDecimal salario = usuario.getSalario(); // Ex: 5000.00

// DIVISÃO com precisão e arredondamento:
BigDecimal percentual = salario.compareTo(BigDecimal.ZERO) > 0
    ? total.divide(salario, 4, RoundingMode.HALF_UP)
    //      ↑       ↑        ↑  ↑
    //      │       │        │  └─ RoundingMode.HALF_UP:
    //      │       │        │     Arredonda 0.5 para cima (padrão matemático)
    //      │       │        │     Ex: 3.33555 → 3.3356 (4 casas)
    //      │       │        └─ Número de casas decimais do resultado
    //      │       └─ BigDecimal pelo qual dividir (salário)
    //      └─ BigDecimal dividendo (total de assinaturas)
    //
    // Se total = 385.50 e salario = 5000.00:
    // 385.50 / 5000.00 = 0.0771 (4 casas decimais)

      .multiply(BigDecimal.valueOf(100))
    //  ↑ Multiplica por 100 para converter para percentual
    // 0.0771 × 100 = 7.7100

    : BigDecimal.ZERO;
// Se salario == 0: percentual = 0 (evita ArithmeticException: divisão por zero)

// Por que compareTo() e não == ou .equals() para zero?
BigDecimal x = new BigDecimal("0");     // scale = 0
BigDecimal y = new BigDecimal("0.00");  // scale = 2
x.equals(y)        // FALSE! ← scale diferente!
x.compareTo(y)     // 0 (IGUAL matematicamente) ← use SEMPRE para BigDecimal
```

### RoundingMode explicado

```
RoundingMode.HALF_UP    (padrão matemático - usado no projeto):
  3.555 → 3.56 (o 5 arredonda para cima)
  3.554 → 3.55 (menor que 5 arredonda para baixo)

RoundingMode.HALF_EVEN  (banqueiro - evita viés):
  3.555 → 3.56 (par mais próximo)
  3.545 → 3.54 (par mais próximo)

RoundingMode.FLOOR      (sempre para baixo):
  3.999 → 3.99

RoundingMode.CEILING    (sempre para cima):
  3.001 → 3.01
```

### `formatar()` em AiService e RelatorioService

```java
private String formatar(BigDecimal valor) {
    return valor != null
        ? valor.setScale(2, RoundingMode.HALF_UP).toPlainString()
        //       ↑ garante sempre 2 casas decimais
        //       Ex: 49.9 → "49.90"
        //           49   → "49.00"
        //       .toPlainString() retorna "49.90" (sem notação científica)
        //       (sem toPlainString, valores muito grandes: "4.99E+2")
        : "0,00";
    //   ↑ fallback se valor for null
}
```

---

## 8. UsuarioService — Linha a Linha

```java
@Service
public class UsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);
    // Logger: usa SLF4J (Simple Logging Facade for Java)
    // getLogger(UsuarioService.class): associa os logs à classe
    // Assim, nos logs aparece: [UsuarioService] Consultando perfil...

    private final UsuarioRepository usuarioRepository;

    // Injeção via construtor (sem @Autowired — Spring 4.3+ injeta automaticamente
    // se há um único construtor)
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // ─────────────── buscarPerfil() ───────────────
    public UsuarioResponseDTO buscarPerfil() {
        logger.info("Consultando perfil do usuário autenticado");
        // logger.info: nível informativo
        // {} é placeholder — não concatena String antes de saber se o log será exibido
        // Mais eficiente que logger.info("Consultando: " + email)

        return UsuarioResponseDTO.fromEntity(getUsuarioAutenticado());
        // 1. getUsuarioAutenticado() → busca o usuário autenticado do SecurityContextHolder
        // 2. fromEntity() → converte Entity para DTO (remove senha)
        // 3. retorna o DTO
    }

    // ─────────────── atualizarSalario() ───────────────
    @Transactional
    // ↑ Tudo neste método executa em uma transação de banco
    // Se lançar exceção: ROLLBACK (desfaz as mudanças)
    // Se completar: COMMIT (confirma no banco)
    public UsuarioResponseDTO atualizarSalario(AtualizarSalarioDTO dto) {
        Usuario usuario = getUsuarioAutenticado();
        // ↑ Busca a entidade do usuário autenticado
        // A entidade está no estado MANAGED (rastreada pelo JPA)

        logger.info("Atualizando salário do usuário {} para {}", usuario.getEmail(), dto.salario());
        // {} {} → dois placeholders: email e novo salário

        usuario.setSalario(dto.salario());
        // ↑ Altera o campo salario na entidade
        // Entidade ainda está MANAGED → JPA rastreia a mudança

        return UsuarioResponseDTO.fromEntity(usuarioRepository.save(usuario));
        // ↑ save(usuario):
        //   - usuario já tem ID (existe no banco)
        //   - JPA detecta: "tem ID → UPDATE"
        //   - Executa: UPDATE usuarios SET salario = ? WHERE id = ?
        //   - Retorna a entidade atualizada (com novo salário confirmado)
        // ↑ fromEntity(): converte a entidade atualizada em DTO
    }

    // ─────────────── listarTodos() ───────────────
    public List<UsuarioResponseDTO> listarTodos() {
        logger.info("Listando todos os usuários cadastrados (operação admin)");

        return usuarioRepository.findAll()
        // ↑ SELECT * FROM usuarios
        // Retorna List<Usuario> com TODOS

                .stream()
                .map(UsuarioResponseDTO::fromEntity)
        // ↑ Cada Usuario → UsuarioResponseDTO (sem senha!)
                .toList();
        // ↑ List imutável com todos os DTOs
    }

    // ─────────────── buscarPorId() ───────────────
    public UsuarioResponseDTO buscarPorId(Long id) {
        logger.info("Buscando usuário por ID: {}", id);

        return usuarioRepository.findById(id)
        // ↑ SELECT * FROM usuarios WHERE id = ?
        // Retorna Optional<Usuario>

                .map(UsuarioResponseDTO::fromEntity)
        // ↑ .map() no Optional:
        //   Se Optional TEM valor: aplica a função (Entity → DTO)
        //   Retorna Optional<UsuarioResponseDTO>
        //   Se Optional VAZIO: continua vazio

                .orElseThrow(() -> {
        // ↑ Se o Optional chegou vazio até aqui:
        //   executa a lambda, lança a exceção retornada
                    logger.warn("Usuário não encontrado com ID: {}", id);
                    return new IllegalArgumentException("Usuário não encontrado.");
                    // GlobalExceptionHandler captura → 400 Bad Request
                });
    }

    // ─────────────── promoverParaAdmin() ───────────────
    @Transactional
    public UsuarioResponseDTO promoverParaAdmin(Long id) {

        // Obtém o email de quem está FAZENDO a requisição
        String emailAutenticado = SecurityContextHolder
            .getContext()          // Contexto do thread atual
            .getAuthentication()   // Objeto Authentication (quem está logado)
            .getName();            // Nome = username = email
        // Ex: "admin@empresa.com"

        logger.info("Admin {} solicitou promoção do usuário ID: {}", emailAutenticado, id);

        // Busca o usuário que SERÁ promovido
        Usuario alvo = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Promoção falhou: usuário ID {} não encontrado", id);
                    return new IllegalArgumentException("Usuário não encontrado.");
                });

        // REGRA DE NEGÓCIO 1: Ninguém pode se promover
        if (alvo.getEmail().equals(emailAutenticado)) {
        // ↑ .equals() compara o CONTEÚDO das Strings (não a referência de memória!)
        //   "admin@email.com".equals("admin@email.com") = true
            logger.warn("Promoção rejeitada: admin {} tentou se auto-promover", emailAutenticado);
            throw new IllegalArgumentException("Você não pode alterar sua própria role.");
        }

        // REGRA DE NEGÓCIO 2: Já é ADMIN, não precisa promover
        if (alvo.getRole() == Role.ADMIN) {
        // ↑ Para enums, use == (não .equals())
        //   Enums são singletons — cada valor existe uma única vez na JVM
        //   Role.ADMIN == Role.ADMIN é sempre true (mesma instância)
            logger.warn("Promoção rejeitada: usuário {} já é ADMIN", alvo.getEmail());
            throw new IllegalArgumentException("Usuário já é administrador.");
        }

        // Altera a role
        alvo.setRole(Role.ADMIN);
        // ↑ Entidade ainda MANAGED, JPA rastreia a mudança

        logger.info("Usuário {} promovido para ADMIN por {}", alvo.getEmail(), emailAutenticado);

        return UsuarioResponseDTO.fromEntity(usuarioRepository.save(alvo));
        // ↑ UPDATE usuarios SET role = 'ADMIN' WHERE id = ?
        // Retorna DTO com role = "ADMIN"
    }

    // ─────────────── getUsuarioAutenticado() ───────────────
    public Usuario getUsuarioAutenticado() {
        String email = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();
        // ↑ Lê o email do usuário autenticado
        // Só funciona porque JwtAuthFilter já populou o SecurityContextHolder
        // Se chamado sem token válido: NullPointerException no .getName()
        // (mas isso nunca acontece — rotas protegidas exigem token)

        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Usuário autenticado não encontrado no banco: {}", email);
                    // logger.error: nível de erro — indica situação anômala
                    return new IllegalStateException("Usuário autenticado não encontrado.");
                    // IllegalStateException (não IllegalArgumentException):
                    // Estado inválido do sistema — não é erro do cliente
                    // Não deveria acontecer: token válido mas usuário deletado?
                });
    }
}
```

---

## 9. AuthService — Linha a Linha

```java
@Service
public class AuthService {

    // Todas as dependências são finais → imutáveis após construção
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    // ↑ Interface. Spring injeta BCryptPasswordEncoder
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    // ↑ Gerenciador central de autenticação do Spring Security
    private final UserDetailsService userDetailsService;
    // ↑ Interface. Spring injeta UserDetailsServiceImpl

    // ─────────────── registrar() ───────────────
    public JwtResponseDTO registrar(RegisterRequestDTO dto) {
        logger.info("Tentativa de registro para o e-mail: {}", dto.email());

        // REGRA DE NEGÓCIO: e-mail único
        if (usuarioRepository.existsByEmail(dto.email())) {
        // ↑ SELECT COUNT(*) > 0 FROM usuarios WHERE email = ?
        // Mais eficiente que findByEmail (não carrega o objeto)
            logger.warn("Registro rejeitado: e-mail já cadastrado - {}", dto.email());
            throw new IllegalArgumentException("E-mail já cadastrado.");
            // GlobalExceptionHandler → 400 Bad Request
            // {"error": "E-mail já cadastrado.", "code": 400}
        }

        // Cria nova entidade (ainda não existe no banco)
        Usuario usuario = new Usuario();
        // Estado JPA: TRANSIENT (sem ID, não rastreado)

        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());

        // SEGURANÇA CRÍTICA: hash da senha
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        // ↑ BCrypt.encode("123456") → "$2a$10$randomSalt/hashedPassword..."
        // Cada execução gera hash DIFERENTE (salt aleatório)
        // Sem isso: senha ficaria em texto puro no banco ← NUNCA faça isso
        // BCrypt é lento por design: protege contra força bruta

        usuario.setSalario(dto.salario());
        // Role padrão já está em Usuario: private Role role = Role.USER;

        // Persiste no banco
        usuarioRepository.save(usuario);
        // ↑ INSERT INTO usuarios (nome, email, senha, salario, role, created_at)
        //   VALUES (?, ?, ?, ?, 'USER', NOW())
        // JPA gera o ID (BIGSERIAL no PostgreSQL)
        // Estado JPA: MANAGED (agora tem ID)

        // Carrega como UserDetails (formato Spring Security)
        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
        // ↑ SELECT * FROM usuarios WHERE email = ?
        // Retorna User(email, senhaHash, [ROLE_USER])
        // Por que buscar de novo? Para garantir que as authorities estão corretas
        // e no formato que JwtUtil espera

        // Gera o token JWT
        String token = jwtUtil.generateToken(userDetails);
        // ↑ Jwts.builder()
        //     .subject("joao@email.com")
        //     .claim("roles", "[ROLE_USER]")
        //     .issuedAt(agora)
        //     .expiration(agora + 86400000ms)
        //     .signWith(chaveSecreta)
        //     .compact()
        // → "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2FvQGVtYWlsLmNvbSJ9.sig"

        logger.info("Usuário registrado com sucesso: {}", usuario.getEmail());

        return new JwtResponseDTO(token, usuario.getEmail(), usuario.getRole().name());
        // ↑ Record imutável com os 3 campos
        // usuario.getRole().name() → Role.USER.name() → "USER"
        // Jackson serializa automaticamente para JSON:
        // { "token": "eyJ...", "email": "joao@email.com", "role": "USER" }
    }

    // ─────────────── login() ───────────────
    public JwtResponseDTO login(LoginRequestDTO dto) {
        logger.info("Tentativa de login para o e-mail: {}", dto.email());

        // VALIDAÇÃO VIA SPRING SECURITY
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(dto.email(), dto.senha())
            // ↑ Cria um "token de autenticação pendente"
            // Contém: principal=email, credentials=senha
        );
        // ↑ O que authenticationManager.authenticate() faz internamente:
        //   1. Recebe o UsernamePasswordAuthenticationToken
        //   2. Delega para DaoAuthenticationProvider (configurado em SecurityConfig)
        //   3. Provider chama: userDetailsService.loadUserByUsername(dto.email())
        //      → SELECT * FROM usuarios WHERE email = ?
        //      → Se não encontrar: UsernameNotFoundException → BadCredentialsException
        //   4. Provider chama: passwordEncoder.matches(dto.senha(), hashDoBanco)
        //      → BCrypt compara a senha fornecida com o hash
        //      → matches("123456", "$2a$10$...") = true ← hash corresponde à senha
        //      → matches("errada", "$2a$10$...") = false
        //   5. Se matches = false: BadCredentialsException
        //      GlobalExceptionHandler → 401 Unauthorized
        //   6. Se matches = true: autenticação bem-sucedida, continua

        // Aqui chegamos APENAS se autenticação foi bem-sucedida
        UserDetails userDetails = userDetailsService.loadUserByUsername(dto.email());
        // ↑ Busca novamente para ter o UserDetails com authorities
        // (O authenticationManager não retorna o UserDetails direto)

        String token = jwtUtil.generateToken(userDetails);
        // ↑ Gera novo JWT assinado

        Usuario usuario = usuarioRepository.findByEmail(dto.email()).orElseThrow();
        // ↑ Busca a entidade completa para pegar o role
        // .orElseThrow() sem argumento: NoSuchElementException
        // Seguro aqui: se chegou até aqui, o usuário existe (authenticate() validou)

        logger.info("Login realizado com sucesso: {}", usuario.getEmail());

        return new JwtResponseDTO(token, usuario.getEmail(), usuario.getRole().name());
    }
}
```

---

## 10. AssinaturaService — Linha a Linha

```java
@Service
public class AssinaturaService {

    private static final Logger logger = LoggerFactory.getLogger(AssinaturaService.class);
    private final AssinaturaRepository assinaturaRepository;
    private final UsuarioService usuarioService;
    // ↑ Service injetando outro Service — permitido quando necessário
    // AssinaturaService PRECISA do usuário autenticado (via UsuarioService)

    // ─────────────── listar() ───────────────
    public List<AssinaturaResponseDTO> listar() {
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        // ↑ Lê email do SecurityContextHolder → busca no banco → retorna Entity

        return assinaturaRepository.findByUsuarioId(usuario.getId())
        // ↑ SELECT * FROM assinaturas WHERE usuario_id = ?
        // Filtra no BANCO — eficiente (não traz assinaturas de outros usuários)
                .stream()
                .map(AssinaturaResponseDTO::fromEntity)
        // ↑ Cada Assinatura (Entity) → AssinaturaResponseDTO
        //   Remove o campo usuario (Entity aninhada)
        //   Converte categoria (enum) → String
                .toList();
    }

    // ─────────────── criar() ───────────────
    @Transactional
    public AssinaturaResponseDTO criar(AssinaturaRequestDTO dto) {
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        logger.info("Criando assinatura '{}' para o usuário: {}", dto.nome(), usuario.getEmail());

        Assinatura assinatura = new Assinatura();
        // Estado: TRANSIENT (sem ID, JPA não rastreia)

        // Popula campos DO dto (não do usuário — segurança)
        assinatura.setNome(dto.nome());         // String do DTO
        assinatura.setValor(dto.valor());       // BigDecimal do DTO
        assinatura.setCategoria(dto.categoria()); // enum do DTO

        assinatura.setUsuario(usuario);
        // ↑ Associa ao usuário autenticado
        // SEGURANÇA: o frontend NÃO define quem é o dono
        //            o dono é sempre quem está autenticado
        // Se você permitisse: dto.usuarioId = 999 (atacante tenta burlar)
        // Com esta linha: assinatura SEMPRE pertence ao usuário atual

        // ativo = true é o default na Entity:
        // private Boolean ativo = true;

        return AssinaturaResponseDTO.fromEntity(assinaturaRepository.save(assinatura));
        // ↑ save(assinatura):
        //   assinatura SEM ID → INSERT
        //   INSERT INTO assinaturas (nome, valor, categoria, ativo, usuario_id, created_at)
        //   VALUES (?, ?, ?, true, ?, NOW())
        //   Retorna assinatura COM ID gerado
        // ↑ fromEntity(): Entity → DTO
        // ↑ @Transactional garante: ou tudo salva, ou nada
    }

    // ─────────────── atualizar() ───────────────
    @Transactional
    public AssinaturaResponseDTO atualizar(Long id, AssinaturaRequestDTO dto) {
        logger.info("Atualizando assinatura ID: {}", id);

        Assinatura assinatura = buscarPorIdDoUsuario(id);
        // ↑ Método privado de segurança:
        //   1. Busca pelo ID
        //   2. Verifica que PERTENCE ao usuário autenticado
        //   3. Retorna a entidade OU lança exceção

        assinatura.setNome(dto.nome());
        assinatura.setValor(dto.valor());
        assinatura.setCategoria(dto.categoria());
        // ↑ Atualiza apenas nome/valor/categoria
        // ativo e usuario NÃO são alteráveis aqui (intencionalmente)

        return AssinaturaResponseDTO.fromEntity(assinaturaRepository.save(assinatura));
        // ↑ save(assinatura):
        //   assinatura TEM ID → UPDATE
        //   UPDATE assinaturas SET nome=?, valor=?, categoria=? WHERE id=?
    }

    // ─────────────── remover() ───────────────
    @Transactional
    public void remover(Long id) {
        logger.info("Removendo assinatura ID: {}", id);

        Assinatura assinatura = buscarPorIdDoUsuario(id);
        // ↑ Segurança: só pode remover a PRÓPRIA assinatura

        assinaturaRepository.delete(assinatura);
        // ↑ DELETE FROM assinaturas WHERE id = ?
        // Não é necessário deletar o usuário junto
        // (a assinatura pertence a um usuário, não o contrário)

        logger.info("Assinatura ID: {} removida com sucesso", id);
    }

    // ─────────────── alternarAtivo() ───────────────
    @Transactional
    public AssinaturaResponseDTO alternarAtivo(Long id) {
        Assinatura assinatura = buscarPorIdDoUsuario(id);
        // ↑ Busca com verificação de propriedade

        boolean novoStatus = !assinatura.getAtivo();
        // ↑ Operador ! (NOT): inverte o booleano
        //   getAtivo() = true  → !true  = false (desativar)
        //   getAtivo() = false → !false = true  (ativar)

        assinatura.setAtivo(novoStatus);

        logger.info("Assinatura ID: {} alterada para {}", id, novoStatus ? "ATIVA" : "INATIVA");
        // ↑ Operador ternário: condição ? seTrue : seFalse
        //   novoStatus ? "ATIVA" : "INATIVA"
        //   Se novoStatus = true: "ATIVA"
        //   Se novoStatus = false: "INATIVA"

        return AssinaturaResponseDTO.fromEntity(assinaturaRepository.save(assinatura));
        // ↑ UPDATE assinaturas SET ativo = ? WHERE id = ?
    }

    // ─────────────── calcularResumoFinanceiro() ───────────────
    public ResumoFinanceiroDTO calcularResumoFinanceiro() {
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        Long usuarioId = usuario.getId();

        // QUERY 1: assinaturas ativas (para listar e para a IA)
        List<AssinaturaResponseDTO> assinaturas = assinaturaRepository
                .findByUsuarioIdAndAtivoTrue(usuarioId)
                // SELECT * FROM assinaturas WHERE usuario_id=? AND ativo=true
                .stream()
                .map(AssinaturaResponseDTO::fromEntity)
                .toList();

        // QUERY 2: soma total (feita no banco, eficiente)
        BigDecimal total = assinaturaRepository.sumValorAtivoByUsuarioId(usuarioId);
        // SELECT SUM(valor) FROM assinaturas WHERE usuario_id=? AND ativo=true
        // Retorna: 385.50 (ou null se não há assinaturas)

        if (total == null) total = BigDecimal.ZERO;
        // BigDecimal.ZERO = BigDecimal com valor 0

        BigDecimal salario = usuario.getSalario();
        // Ex: 5000.00

        // CÁLCULO DO PERCENTUAL
        BigDecimal percentual = salario.compareTo(BigDecimal.ZERO) > 0
            ? total.divide(salario, 4, RoundingMode.HALF_UP)
                   .multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        // 385.50 / 5000.00 = 0.0771 → × 100 = 7.7100

        // QUERY 3: gastos agrupados por categoria
        Map<String, BigDecimal> gastosPorCategoria = new LinkedHashMap<>();
        // LinkedHashMap: mantém ordem de inserção
        // (categorias aparecem sempre na mesma ordem)
        // HashMap: ordem aleatória (não use para dados que precisam de ordem)

        assinaturaRepository.sumValorGroupedByCategoriaAndUsuarioId(usuarioId)
        // SELECT a.categoria, SUM(a.valor)
        // FROM assinatura a
        // WHERE a.usuario.id = ? AND a.ativo = true
        // GROUP BY a.categoria
        // Retorna: [[STREAMING_VIDEO, 250.00], [STREAMING_MUSICA, 80.00], ...]
                .forEach(row -> gastosPorCategoria.put(
                    ((CategoriaAssinatura) row[0]).name(),
                    // row[0] = CategoriaAssinatura.STREAMING_VIDEO (enum)
                    // .name() = "STREAMING_VIDEO" (String)
                    (BigDecimal) row[1]
                    // row[1] = 250.00 (BigDecimal direto do PostgreSQL NUMERIC)
                ));

        // Monta o DTO com todos os dados calculados
        return new ResumoFinanceiroDTO(salario, total, percentual, assinaturas, gastosPorCategoria);
        // Record imutável criado com o construtor gerado automaticamente
    }

    // ─────────────── buscarPorIdDoUsuario() ───────────────
    private Assinatura buscarPorIdDoUsuario(Long id) {
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        // ↑ Quem está fazendo a requisição?

        return assinaturaRepository.findById(id)
        // ↑ SELECT * FROM assinaturas WHERE id = ?
        // Retorna Optional<Assinatura>
        // Pode ser de QUALQUER usuário — por isso verificamos abaixo

                .filter(a -> a.getUsuario().getId().equals(usuario.getId()))
        // ↑ Filter no Optional:
        //   a = a assinatura encontrada no banco
        //   a.getUsuario() = o dono da assinatura (Entity Usuario)
        //   Aqui é SEGURO chamar getUsuario() porque:
        //   - Ainda estamos dentro da transação (se tiver @Transactional na cadeia)
        //   - O Hibernate pode fazer lazy load
        //   .getId() = ID do dono
        //   .equals(usuario.getId()) = compara com o ID do usuário autenticado
        //
        //   Se ID bate: filter retorna Optional com a assinatura
        //   Se não bate: filter retorna Optional.empty()
        //   Se findById retornou vazio: filter mantém vazio

                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada."));
        // ↑ Optional vazio? Lança exceção
        //   Mensagem INTENCIONAL: não diz "Acesso negado"
        //   Segurança por obscuridade: atacante não sabe se o ID existe ou se não tem permissão
    }
}
```

---

## 11. @Transactional — AOP Por Baixo dos Panos

`@Transactional` é implementado via **AOP (Aspect-Oriented Programming)**. O Spring cria um **Proxy** em torno da sua classe.

### Como o Proxy funciona

```
Sem @Transactional — chamada direta:
  Controller → AssinaturaService.criar()

Com @Transactional — Spring intercepta:
  Controller → [Proxy de AssinaturaService] → AssinaturaService.criar()
               ↑ Spring gera esta classe em runtime!
```

**O que o Proxy faz:**

```java
// O Spring gera algo equivalente a isso em runtime:
public class AssinaturaService$$SpringProxy extends AssinaturaService {

    @Override
    public AssinaturaResponseDTO criar(AssinaturaRequestDTO dto) {

        // BEFORE: inicia a transação
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        // Abre conexão com o banco
        // BEGIN TRANSACTION (no PostgreSQL: BEGIN;)

        try {
            // PROCEDE: chama o método REAL
            AssinaturaResponseDTO resultado = super.criar(dto);

            // AFTER SUCCESS: confirma
            transactionManager.commit(status);
            // COMMIT; no PostgreSQL

            return resultado;

        } catch (RuntimeException ex) {
            // AFTER EXCEPTION: reverte
            transactionManager.rollback(status);
            // ROLLBACK; no PostgreSQL
            throw ex; // Repropaga a exceção
        }
    }
}
```

**Por isso `@Transactional` em métodos privados NÃO funciona:**
```java
@Transactional          // ← IGNORADO! Proxy só intercepta métodos públicos
private void metodoPrivado() { ... }

@Transactional          // ← FUNCIONA
public void metodPublico() { ... }
```

**Propagação de transação:**
```java
// AssinaturaService.criar() tem @Transactional
// Dentro dele chama: usuarioService.getUsuarioAutenticado()
// getUsuarioAutenticado() NÃO tem @Transactional

// O que acontece?
// A transação de criar() "propaga" para getUsuarioAutenticado()
// Todos os acessos ao banco dentro de criar() usam a MESMA transação
// Se getUsuarioAutenticado() falhar: ROLLBACK de tudo (incluindo o save da assinatura)
```

---

## 12. SecurityContextHolder — ThreadLocal Explicado

`SecurityContextHolder` armazena o usuário autenticado por **thread** (por requisição).

### O que é ThreadLocal?

```
Servidor recebe 3 requisições simultâneas:
  Thread A: João está logado    → SecurityContextHolder da Thread A tem João
  Thread B: Maria está logada   → SecurityContextHolder da Thread B tem Maria
  Thread C: Pedro está logado   → SecurityContextHolder da Thread C tem Pedro

ThreadLocal = variável cujo valor é SEPARADO por thread
              Thread A lê SecurityContextHolder → vê João (não Maria, não Pedro)
              Thread B lê SecurityContextHolder → vê Maria (não João, não Pedro)

SEM ThreadLocal (imagine um campo static compartilhado):
  Thread A seta: usuarioAtual = João
  Thread B seta: usuarioAtual = Maria  ← sobrescreve!
  Thread A lê: usuarioAtual → Maria    ← ERRADO! João leria dados de Maria
```

### Como o dado chega ao SecurityContextHolder

```
1. Requisição HTTP chega com: Authorization: Bearer eyJ...

2. JwtAuthFilter intercepta:
   email = jwtUtil.extractEmail(token)  → "joao@email.com"
   userDetails = userDetailsService.loadUserByUsername(email)
   authToken = new UsernamePasswordAuthenticationToken(userDetails, null, authorities)
   SecurityContextHolder.getContext().setAuthentication(authToken)
   ↑ ARMAZENA no ThreadLocal da thread atual

3. Spring processa a requisição na MESMA thread

4. Service chama:
   SecurityContextHolder.getContext().getAuthentication().getName()
   ↑ LÊ do ThreadLocal — obtém "joao@email.com"

5. Após a requisição terminar:
   SecurityContextHolder.clearContext()
   ↑ Limpa o ThreadLocal (Spring faz isso automaticamente)
   ↑ Evita vazamento de dados entre requisições
```

---

## 13. RelatorioService — Linha a Linha

```java
@Service
public class RelatorioService {

    // Constantes de cor (Bootstrap 5 palette)
    private static final Color COR_PRIMARIA  = new Color(13, 110, 253);  // #0d6efd (azul)
    private static final Color COR_CINZA     = new Color(108, 117, 125); // #6c757d
    private static final Color COR_CABECALHO = new Color(248, 249, 250); // #f8f9fa (cinza claro)
    // new Color(R, G, B): componentes RGB 0-255
    // static final: constantes de classe, existem uma única vez (não por instância)

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // DateTimeFormatter: formata datas
    // ofPattern("dd/MM/yyyy"): define o padrão
    // dd = dia com 2 dígitos, MM = mês com 2 dígitos, yyyy = ano com 4 dígitos
    // Ex: LocalDate.now() → "26/03/2026"

    // ─────────────── gerarRelatorioPdf() ───────────────
    public byte[] gerarRelatorioPdf() {
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        logger.info("Gerando relatório PDF para o usuário: {}", usuario.getEmail());

        ResumoFinanceiroDTO resumo = assinaturaService.calcularResumoFinanceiro();
        // ↑ Reutiliza o mesmo método do AssinaturaService
        // DRY (Don't Repeat Yourself): lógica de cálculo em um lugar só

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // ↑ Stream que escreve em MEMÓRIA (não em arquivo)
        // Como um array de bytes que cresce conforme você escreve
        // No final: out.toByteArray() retorna tudo que foi escrito

        Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
        // ↑ Documento PDF A4
        // Margens: esquerda=40, direita=40, topo=50, base=40 (em pontos)
        // 1 ponto = 1/72 de polegada ≈ 0.35mm

        PdfWriter.getInstance(doc, out);
        // ↑ Liga o Document ao ByteArrayOutputStream
        // Tudo que for adicionado ao doc vai para o out (memória)

        doc.open();
        // ↑ Abre o documento para escrita (começa o PDF)

        // Seções do PDF (cada uma é um método privado)
        adicionarCabecalho(doc, usuario);     // "GeriStreams" + nome + email + data
        adicionarResumo(doc, resumo);         // Tabela: salário, total, %
        adicionarTabelaAssinaturas(doc, resumo); // Tabela: nome | categoria | valor
        adicionarGastosPorCategoria(doc, resumo); // Tabela: categoria | total
        adicionarRodape(doc);                 // Linha + mensagem de rodapé

        doc.close();
        // ↑ Finaliza o PDF (escreve o footer do PDF, referências cruzadas, etc.)
        // IMPORTANTE: só feche depois de adicionar tudo
        // Depois do close, não é possível adicionar mais nada

        logger.info("Relatório PDF gerado com sucesso ({} bytes)", out.size());
        return out.toByteArray();
        // ↑ Retorna todos os bytes do PDF
        // O Controller envia esses bytes como response body
        // Content-Type: application/pdf diz ao browser como tratar
    }

    // ─────────────── adicionarTabelaAssinaturas() ───────────────
    private void adicionarTabelaAssinaturas(Document doc, ResumoFinanceiroDTO resumo) throws DocumentException {

        if (resumo.assinaturas().isEmpty()) return;
        // ↑ Se não há assinaturas ativas: pula a seção inteira
        // Early return: evita if/else aninhado

        PdfPTable tabela = new PdfPTable(3);
        // ↑ Tabela com 3 colunas: Serviço | Categoria | Valor/mês
        tabela.setWidthPercentage(100);      // Ocupa 100% da largura
        tabela.setWidths(new float[]{4, 3, 2}); // Proporções: 4:3:2
        // Coluna 1 (Serviço): 4 partes → mais larga (nomes podem ser longos)
        // Coluna 2 (Categoria): 3 partes
        // Coluna 3 (Valor): 2 partes → mais estreita (números são curtos)

        adicionarCabecalhoTabela(tabela, "Serviço", "Categoria", "Valor/mês");

        Font fonteCell = FontFactory.getFont(FontFactory.HELVETICA, 10);
        boolean linha = false;
        // ↑ Flag para alternar cor de fundo das linhas (zebra striping)

        for (AssinaturaResponseDTO a : resumo.assinaturas()) {
            Color bg = linha ? Color.WHITE : COR_CABECALHO;
            // linha=false → bg=COR_CABECALHO (cinza claro) → linha 1
            // linha=true  → bg=Color.WHITE   (branco)       → linha 2
            // linha=false → bg=COR_CABECALHO                → linha 3
            // etc.

            adicionarCelulaTabela(tabela, a.nome(), fonteCell, bg);
            adicionarCelulaTabela(tabela, a.categoria(), fonteCell, bg);
            adicionarCelulaTabela(tabela, "R$ " + formatar(a.valor()), fonteCell, bg);

            linha = !linha; // Inverte para próxima linha
        }

        doc.add(tabela);
    }
}
```

---

## 14. AiService — Linha a Linha e Integração IA

Esta é a integração mais complexa do projeto. Vamos destrinchar cada parte.

### O que é o RestClient?

```
RestClient (Spring 6.1+) é uma API fluente para fazer requisições HTTP.
É como um HttpClient sofisticado que:
  - Configura URL base
  - Adiciona headers padrão
  - Serializa/deserializa JSON automaticamente (via Jackson)
  - Lança exceções para status de erro (4xx, 5xx)
```

### Configuração do RestClient (RestClientConfig)

```java
@Configuration
public class RestClientConfig {

    @Value("${anthropic.api.url}")
    private String anthropicUrl;         // "https://api.anthropic.com"

    @Value("${anthropic.api.key}")
    private String anthropicApiKey;      // "sk-ant-api03-..."

    @Value("${anthropic.api.version}")
    private String anthropicVersion;     // "2023-06-01"

    @Bean("anthropicRestClient")
    // ↑ Nomeia o bean "anthropicRestClient"
    // Permite injetar especificamente este RestClient com @Qualifier
    public RestClient anthropicRestClient() {
        return RestClient.builder()
            .baseUrl(anthropicUrl)
            // ↑ Toda requisição começa com "https://api.anthropic.com"
            // .uri("/v1/messages") na chamada → "https://api.anthropic.com/v1/messages"

            .defaultHeader("x-api-key", anthropicApiKey)
            // ↑ Header de autenticação da API Anthropic
            // Obrigatório em TODA requisição (por isso defaultHeader)
            // A chave secreta que identifica sua conta Anthropic

            .defaultHeader("anthropic-version", anthropicVersion)
            // ↑ Versão da API: "2023-06-01"
            // Anthropic usa versionamento por data
            // Garante que a API não mude de comportamento inesperadamente

            .defaultHeader("Content-Type", "application/json")
            // ↑ Diz para a API: "estou enviando JSON"

            .build();
    }
}
```

### AiService — Estrutura Completa

```java
@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    // System Prompt: instrução de comportamento para o Claude
    // Separado das mensagens do usuário
    private static final String SYSTEM_PROMPT = """
        Você é um consultor financeiro pessoal especializado em assinaturas de streaming e serviços digitais.
        Seu tom é amigável, direto e encorajador.
        Sempre responda em português brasileiro.
        use **negrito** para pontos importantes e listas numeradas para as dicas.
        Não use emojis em excesso, apenas onde fizer sentido.
        Não pule muitas linhas faça um texto bem divido mas sem tanto espaçamento entre as linhas.
        """;
    // Text block (""") = string multilinha Java 15+
    // O conteúdo entre as aspas triplas é o valor exato da string
    // Preserva identação relativa

    private final RestClient anthropicRestClient;
    // ↑ @Qualifier("anthropicRestClient") no construtor garante que
    //   o Spring injete especificamente o bean nomeado "anthropicRestClient"
    //   (e não outro RestClient que possa existir)

    @Value("${anthropic.model}")
    private String model;
    // ↑ Injetado do application.properties: anthropic.model=claude-haiku-4-5
    // Permite mudar o modelo sem recompilar o código

    public AiService(@Qualifier("anthropicRestClient") RestClient anthropicRestClient,
                     AssinaturaService assinaturaService,
                     UsuarioService usuarioService) {
        this.anthropicRestClient = anthropicRestClient;
        // ...
    }

    // ─────────────── gerarDicas() ───────────────
    public AiDicasResponseDTO gerarDicas() {

        // PASSO 1: Buscar dados financeiros do usuário
        ResumoFinanceiroDTO resumo = assinaturaService.calcularResumoFinanceiro();
        // ↑ Reutiliza o cálculo completo: salário, total, %, assinaturas, categorias

        Usuario usuario = usuarioService.getUsuarioAutenticado();
        // ↑ Busca a entidade para ter o NOME (para personalizar o prompt)

        logger.info("Gerando dicas de IA para o usuário: {}", usuario.getEmail());

        // PASSO 2: Construir o prompt personalizado
        String prompt = construirPrompt(usuario.getNome(), resumo);
        // ↑ Texto que será enviado como mensagem do usuário para o Claude

        // PASSO 3: Montar o corpo da requisição
        AnthropicRequestDTO requestBody = new AnthropicRequestDTO(
            model,          // "claude-haiku-4-5" — modelo mais rápido e barato
            1024,           // max_tokens: limite de tokens na resposta
                            // 1 token ≈ 4 caracteres em inglês
                            // 1024 tokens ≈ ~750 palavras
            SYSTEM_PROMPT,  // Instrução de comportamento
            List.of(new AnthropicMessageDTO("user", prompt)),
            // ↑ Array de mensagens (formato conversacional)
            //   role: "user" = mensagem vinda do usuário
            //   content: o prompt personalizado
            //   (poderia ter "assistant" também para multi-turn conversations)
            false           // stream: false = resposta completa (não streaming)
                            // true = Server-Sent Events (tokens chegam em tempo real)
        );
        // Jackson serializa para JSON:
        // {
        //   "model": "claude-haiku-4-5",
        //   "max_tokens": 1024,
        //   "system": "Você é um consultor...",
        //   "messages": [{ "role": "user", "content": "Olá! Me chamo João..." }],
        //   "stream": false
        // }
        // Nota: maxTokens → max_tokens por causa do @JsonProperty("max_tokens")

        // PASSO 4: Chamar a API Anthropic
        AnthropicResponseDTO response = anthropicRestClient
            .post()
            // ↑ Método HTTP: POST

            .uri("/v1/messages")
            // ↑ URI relativa: + baseUrl = "https://api.anthropic.com/v1/messages"

            .body(requestBody)
            // ↑ Jackson serializa AnthropicRequestDTO → JSON automaticamente
            // Envia no corpo da requisição HTTP

            .retrieve()
            // ↑ Executa a requisição e obtém a resposta
            // Se status 4xx ou 5xx: lança HttpClientErrorException

            .body(AnthropicResponseDTO.class);
            // ↑ Jackson desserializa o JSON da resposta → AnthropicResponseDTO
            // O JSON retornado pela Anthropic:
            // {
            //   "id": "msg_01ABC...",
            //   "content": [
            //     { "type": "text", "text": "**Análise do seu perfil:**\n\n..." }
            //   ],
            //   "stop_reason": "end_turn",
            //   "model": "claude-haiku-4-5-20251001"
            // }

        if (response == null) {
            logger.error("API de IA retornou resposta nula para o usuário: {}", usuario.getEmail());
            throw new IllegalStateException("Nenhuma resposta recebida da API de IA.");
        }

        logger.info("Dicas de IA geradas com sucesso para o usuário: {}", usuario.getEmail());

        return new AiDicasResponseDTO(response.extractText());
        // ↑ extractText() navega pelo array content[]
        //   Encontra o bloco de tipo "text"
        //   Retorna o texto gerado pelo Claude (Markdown)
        // AiDicasResponseDTO { dicas: "**Análise:** ..." }
        // Jackson serializa: { "dicas": "**Análise:** ..." }
        // Frontend recebe e usa marked.parse() para converter Markdown → HTML
    }

    // ─────────────── construirPrompt() ───────────────
    private String construirPrompt(String nome, ResumoFinanceiroDTO resumo) {
        StringBuilder sb = new StringBuilder();
        // ↑ StringBuilder: buffer mutável para construção de Strings
        // EFICIENTE: não cria novas Strings a cada concatenação
        // String + String + String: cria objeto intermediário a cada +
        // StringBuilder.append().append(): acumula no mesmo buffer

        sb.append("Olá! Me chamo ").append(nome).append(".\n\n");
        // ↑ .append() retorna o próprio StringBuilder (fluent interface)
        //   Permite encadear múltiplos .append()
        //   \n = newline (quebra de linha)
        //   \n\n = parágrafo (linha em branco)

        sb.append("**Minha situação financeira atual com assinaturas:**\n");
        // ↑ ** em torno do texto = negrito em Markdown
        //   O system prompt instrui o Claude a responder em Markdown

        sb.append("- Salário mensal: R$ ").append(formatar(resumo.salario())).append("\n");
        // resumo.salario() = BigDecimal (ex: 5000.00)
        // formatar(5000.00) = "5000.00" (String formatada)

        sb.append("- Gasto total mensal em assinaturas ativas: R$ ")
          .append(formatar(resumo.totalMensal())).append("\n");

        sb.append("- Percentual do salário comprometido: ")
          .append(resumo.percentualDoSalario().setScale(1, RoundingMode.HALF_UP))
          // ↑ .setScale(1, HALF_UP): arredonda para 1 casa decimal
          //   7.7100 → 7.7
          .append("%\n\n");

        // Lista de assinaturas ativas
        if (!resumo.assinaturas().isEmpty()) {
            sb.append("**Minhas assinaturas ativas:**\n");
            resumo.assinaturas().forEach(a ->
                sb.append("- ").append(a.nome())
                  .append(" (").append(a.categoria()).append(")")
                  .append(" — R$ ").append(formatar(a.valor())).append("/mês\n")
            );
            // ↑ forEach com lambda:
            //   Para cada AssinaturaResponseDTO 'a' na lista:
            //   "- Netflix (STREAMING_VIDEO) — R$ 49.90/mês\n"
            sb.append("\n");
        }

        // Gastos por categoria
        if (!resumo.gastosPorCategoria().isEmpty()) {
            sb.append("**Gastos por categoria:**\n");
            resumo.gastosPorCategoria().forEach((categoria, valor) ->
            // ↑ Map.forEach: lambda com 2 parâmetros (chave, valor)
                sb.append("- ").append(categoria)
                  .append(": R$ ").append(formatar(valor)).append("\n")
            );
            // "- STREAMING_VIDEO: R$ 250.00\n"
            sb.append("\n");
        }

        // Instrução final para o Claude
        sb.append("Com base nessas informações, me dê 1 dicas práticas e personalizadas ")
          .append("para eu reduzir meus gastos com assinaturas e melhorar meu orçamento. ")
          .append("Se alguma assinatura parecer redundante ou cara, mencione especificamente. ")
          .append("Seja objetivo e mostre o impacto financeiro de cada sugestão quando possível.");

        return sb.toString();
        // ↑ Converte StringBuilder → String final
        // Resultado: prompt completo com todos os dados do usuário
    }
}
```

---

## 15. Jackson — Serialização e Deserialização JSON

**Jackson** é a biblioteca que o Spring Boot usa para converter Java ↔ JSON automaticamente.

### Serialização (Java → JSON)

```
Quando o Controller retorna um objeto Java:
  Controller retorna: new AiDicasResponseDTO("**Análise:** ...")

Jackson detecta o tipo (AiDicasResponseDTO)
Jackson lê os campos (via reflexão ou record accessor methods)
Jackson gera JSON:
  { "dicas": "**Análise:** ..." }

Spring escreve o JSON no corpo da resposta HTTP com Content-Type: application/json
```

### Deserialização (JSON → Java)

```
Quando chega uma requisição POST com JSON:
  Body: { "nome": "Netflix", "valor": 49.90, "categoria": "STREAMING_VIDEO" }

Spring detecta @RequestBody AssinaturaRequestDTO
Jackson lê o JSON
Jackson cria: new AssinaturaRequestDTO("Netflix", new BigDecimal("49.90"), STREAMING_VIDEO)
  ↑ Enum: "STREAMING_VIDEO" → CategoriaAssinatura.STREAMING_VIDEO (automático)
  ↑ Number: 49.90 → BigDecimal (automático por causa do tipo declarado)

Controller recebe o objeto já populado
```

### `@JsonProperty` — Por que é Necessário

```java
// AnthropicRequestDTO:
public record AnthropicRequestDTO(
    String model,

    @JsonProperty("max_tokens")  // ← ANOTAÇÃO IMPORTANTE
    int maxTokens,
    // ...
)

// Sem @JsonProperty, Jackson usaria o nome do campo Java: "maxTokens"
// O JSON gerado seria: { "model": "...", "maxTokens": 1024, ... }

// Mas a API Anthropic espera: { "model": "...", "max_tokens": 1024, ... }
// (snake_case, não camelCase)

// Com @JsonProperty("max_tokens"):
// Jackson usa "max_tokens" no JSON, mesmo que o campo Java seja "maxTokens"
// { "model": "...", "max_tokens": 1024, ... } ← correto para a Anthropic!

// Mesma lógica para AnthropicResponseDTO:
@JsonProperty("stop_reason")
String stopReason;
// JSON da Anthropic vem: { "stop_reason": "end_turn" }
// Jackson mapeia para: stopReason = "end_turn"
```

### Validação de entrada com Bean Validation

```java
// AssinaturaRequestDTO:
public record AssinaturaRequestDTO(
    @NotBlank(message = "Nome do serviço é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    String nome,

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    BigDecimal valor,

    @NotNull(message = "Categoria é obrigatória")
    CategoriaAssinatura categoria
) {}
```

**Como funciona a validação:**

```
1. Controller recebe: @Valid @RequestBody AssinaturaRequestDTO dto
2. Spring (via Hibernate Validator) verifica cada anotação:
   @NotBlank: nome == null ou nome.isBlank()? → erro
   @Size(max=100): nome.length() > 100? → erro
   @NotNull: valor == null? → erro
   @DecimalMin("0.01"): valor < 0.01? → erro
   @NotNull: categoria == null? → erro
3. Se houver erros:
   Spring lança MethodArgumentNotValidException
   GlobalExceptionHandler captura
   Retorna 400 com detalhes de cada campo inválido
4. Se não houver erros:
   dto chega ao Controller com valores garantidos válidos
```

---

## 16. O Caminho Completo de uma Requisição

Vamos traçar o caminho **exato** de `POST /api/subscriptions` (criar assinatura):

```
[ANGULAR]
  form.getRawValue() = { nome: "Netflix", valor: 49.90, categoria: "STREAMING_VIDEO" }
  JSON.stringify() → '{"nome":"Netflix","valor":49.90,"categoria":"STREAMING_VIDEO"}'

  jwtInterceptor intercepta:
  req.clone({ setHeaders: { Authorization: "Bearer eyJ..." } })

  HTTP: POST http://localhost:8080/api/subscriptions
  Headers: Authorization: Bearer eyJ..., Content-Type: application/json
  Body: {"nome":"Netflix","valor":49.90,"categoria":"STREAMING_VIDEO"}

────────────────────────── REDE ──────────────────────────

[SPRING BOOT]
  1. DispatcherServlet recebe a requisição

  2. CorsFilter:
     Origin: http://localhost:4200 → está em app.cors.allowed-origins? SIM
     Adiciona: Access-Control-Allow-Origin: http://localhost:4200

  3. JwtAuthFilter:
     authHeader = "Bearer eyJ..."
     token = "eyJ..."
     email = jwtUtil.extractEmail(token) → "joao@email.com"
     userDetails = userDetailsService.loadUserByUsername("joao@email.com")
       └─ SELECT * FROM usuarios WHERE email = 'joao@email.com'
       └─ Retorna User("joao@email.com", "$2a$10$hash...", [ROLE_USER])
     jwtUtil.isTokenValid(token, userDetails) = true
     authToken = new UsernamePasswordAuthenticationToken(userDetails, null, [ROLE_USER])
     SecurityContextHolder.getContext().setAuthentication(authToken)

  4. AuthorizationFilter:
     Rota /api/subscriptions → .anyRequest().authenticated()
     getAuthentication() != null? SIM → PERMITE

  5. AssinaturaController.criar(@Valid @RequestBody AssinaturaRequestDTO dto):
     Jackson desserializa:
       {"nome":"Netflix","valor":49.90,"categoria":"STREAMING_VIDEO"}
       → AssinaturaRequestDTO(nome="Netflix", valor=49.90, categoria=STREAMING_VIDEO)
     @Valid valida:
       @NotBlank: nome="Netflix" → OK
       @NotNull + @DecimalMin: valor=49.90 >= 0.01 → OK
       @NotNull: categoria=STREAMING_VIDEO → OK
     Chama: assinaturaService.criar(dto)
     Spring Proxy: inicia transação (@Transactional)

  6. AssinaturaService.criar(dto):
     usuarioService.getUsuarioAutenticado():
       SecurityContextHolder.getContext().getAuthentication().getName()
       → "joao@email.com"
       usuarioRepository.findByEmail("joao@email.com")
       SELECT * FROM usuarios WHERE email = 'joao@email.com'
       → Usuario(id=3, nome="João", email="joao@email.com", ...)

     logger.info("Criando assinatura 'Netflix' para o usuário: joao@email.com")

     assinatura = new Assinatura()           // TRANSIENT
     assinatura.setNome("Netflix")
     assinatura.setValor(49.90)
     assinatura.setCategoria(STREAMING_VIDEO)
     assinatura.setUsuario(usuario)          // usuario.id = 3
     // assinatura.ativo = true (default)

     assinaturaRepository.save(assinatura):
       assinatura sem ID → INSERT
       INSERT INTO assinaturas (nome, valor, categoria, ativo, usuario_id, created_at)
       VALUES ('Netflix', 49.90, 'STREAMING_VIDEO', true, 3, NOW())
       PostgreSQL gera id = 7 (BIGSERIAL)
       Retorna assinatura com id=7

     AssinaturaResponseDTO.fromEntity(assinatura):
       return new AssinaturaResponseDTO(
         7L,                    // id
         "Netflix",             // nome
         new BigDecimal("49.90"), // valor
         "STREAMING_VIDEO",     // categoria (enum.name() → String)
         true,                  // ativo
         LocalDateTime.now()    // createdAt
       )

     Spring Proxy: COMMIT (nenhuma exceção → confirma no banco)

  7. AssinaturaController recebe AssinaturaResponseDTO
     return ResponseEntity.status(HttpStatus.CREATED).body(dto)
     → Status: 201 CREATED
     → Body: AssinaturaResponseDTO(id=7, nome="Netflix", ...)

  8. Jackson serializa AssinaturaResponseDTO → JSON:
     {
       "id": 7,
       "nome": "Netflix",
       "valor": 49.90,
       "categoria": "STREAMING_VIDEO",
       "ativo": true,
       "createdAt": "2026-03-26T14:30:00"
     }

────────────────────────── REDE ──────────────────────────

[ANGULAR]
  HTTP Response 201 CREATED
  Body: {"id":7,"nome":"Netflix","valor":49.90,"categoria":"STREAMING_VIDEO","ativo":true,"createdAt":"..."}

  subscribe({ next: () => {
    this.fecharFormulario()  // fecha o modal/form
    this.carregar()          // GET /api/subscriptions → recarrega lista
    this.loading.set(false)  // esconde spinner
  }})

  assinaturas.set(novaLista) ← signal atualizado
  Template re-renderiza: nova assinatura aparece na lista
```

---

## Resumo Visual — Service como Orquestrador

```
┌─────────────────────────────────────────────────────────────────────┐
│                    AssinaturaService.criar()                        │
│                                                                     │
│  1. UsuarioService.getUsuarioAutenticado()                         │
│     └─ SecurityContextHolder → email → usuarioRepository.findByEmail│
│        └─ Usuario (Entity) com id, nome, email, salario            │
│                                                                     │
│  2. new Assinatura()                                               │
│     dto.nome() → assinatura.setNome()           ← DTO → Entity    │
│     dto.valor() → assinatura.setValor()         ← DTO → Entity    │
│     dto.categoria() → assinatura.setCategoria() ← DTO → Entity    │
│     usuario → assinatura.setUsuario()           ← SEGURANÇA       │
│                                                                     │
│  3. assinaturaRepository.save(assinatura)                          │
│     └─ INSERT INTO assinaturas...               ← Entity → Banco   │
│     └─ Retorna Entity com ID gerado                                │
│                                                                     │
│  4. AssinaturaResponseDTO.fromEntity(assinatura)                   │
│     └─ id, nome, valor, categoria.name(), ativo, createdAt        │
│     └─ SEM usuario (segurança/lazy)             ← Entity → DTO    │
│                                                                     │
│  5. Retorna AssinaturaResponseDTO                                  │
│     └─ Controller → ResponseEntity.status(201).body(dto)          │
│     └─ Jackson → JSON                           ← DTO → JSON      │
└─────────────────────────────────────────────────────────────────────┘

Transformações que acontecem:
  JSON (entrada) → DTO de entrada → Entity JPA → Banco → Entity JPA → DTO de saída → JSON (saída)
  ↑ @Valid            ↑ Service         ↑ Repository        ↑ fromEntity()   ↑ Jackson
```
