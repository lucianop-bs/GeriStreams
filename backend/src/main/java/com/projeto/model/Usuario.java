package com.projeto.model;

// Importações das anotações JPA (Jakarta Persistence API) — responsáveis pelo mapeamento
// objeto-relacional (ORM): transformar uma classe Java em uma tabela do banco de dados.
import jakarta.persistence.*;

// Importações do Lombok — biblioteca que gera código boilerplate automaticamente em tempo de compilação.
// "Boilerplate" é aquele código repetitivo e chato: getters, setters, construtores, etc.
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// BigDecimal é a classe correta para representar valores monetários em Java.
// NUNCA use double ou float para dinheiro! Esses tipos têm imprecisão de ponto flutuante.
// Exemplo: 0.1 + 0.2 em double pode resultar em 0.30000000000000004 — inaceitável para finanças.
import java.math.BigDecimal;

// LocalDateTime representa data + hora sem informação de fuso horário.
// É a forma moderna (Java 8+) de trabalhar com datas. Evite java.util.Date, é legado.
import java.time.LocalDateTime;

// ArrayList é a implementação padrão de List — usamos para inicializar a coleção de assinaturas.
// Sempre inicialize coleções com uma lista vazia para evitar NullPointerException.
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade JPA que representa um usuário do sistema GeriStreams.
 *
 * Esta classe é mapeada diretamente para a tabela "usuarios" no banco de dados PostgreSQL.
 * É a peça central do domínio: todo usuário tem um salário e uma lista de assinaturas.
 *
 * Padrão de projeto aplicado: Entity (camada de modelo/domínio)
 * → Segue a arquitetura em camadas: Entity → Repository → Service → Controller
 * → Esta classe NÃO deve ser exposta diretamente nos Controllers.
 *   Para isso usamos DTOs (Data Transfer Objects) como UsuarioResponseDTO.
 */

// @Entity: marca esta classe como uma entidade JPA.
// O JPA/Hibernate vai criar/gerenciar uma tabela no banco correspondendo a esta classe.
// Sem esta anotação, o Spring Data JPA simplesmente ignora a classe.
@Entity

// @Table(name = "usuarios"): define explicitamente o nome da tabela no banco de dados.
// Sem esta anotação, o JPA usaria o nome da classe ("Usuario") como nome da tabela.
// Boas práticas: use nomes em minúsculas e plural para tabelas (convenção SQL).
@Table(name = "usuarios")

// @Getter: o Lombok gera automaticamente todos os métodos getter (getNome(), getEmail(), etc.)
// Sem Lombok, você precisaria escrever cada getter manualmente — trabalhoso e verboso.
@Getter

// @Setter: o Lombok gera automaticamente todos os métodos setter (setNome(), setEmail(), etc.)
// Os setters são necessários para que o Spring/JPA consiga popular os campos do objeto.
@Setter

// @NoArgsConstructor: gera um construtor sem argumentos (construtor padrão).
// O JPA EXIGE um construtor sem argumentos para poder instanciar a entidade durante
// o carregamento dos dados do banco (via reflection). Sem ele, ocorre erro em tempo de execução.
@NoArgsConstructor
public class Usuario {

    // @Id: marca este campo como a chave primária da tabela.
    // Toda entidade JPA DEVE ter exatamente um campo anotado com @Id.
    @Id

    // @GeneratedValue(strategy = GenerationType.IDENTITY): define que o banco de dados
    // é responsável por gerar automaticamente o valor do ID (auto-incremento).
    // IDENTITY = usa o recurso SERIAL/BIGSERIAL do PostgreSQL (ou AUTO_INCREMENT no MySQL).
    // O banco incrementa automaticamente: 1, 2, 3, 4... a cada novo registro inserido.
    // Alternativas: SEQUENCE (mais performática para inserções em lote), TABLE, AUTO.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column(nullable = false, length = 100):
    // → nullable = false: este campo NÃO pode ser nulo no banco. Gera constraint NOT NULL.
    // → length = 100: limita o campo a 100 caracteres no banco (VARCHAR(100)).
    //   Isso economiza espaço em disco e garante que nomes absurdamente longos sejam rejeitados.
    @Column(nullable = false, length = 100)
    private String nome;

    // unique = true: cria um índice UNIQUE no banco, garantindo que dois usuários
    // não possam ter o mesmo e-mail. É a regra de negócio mais básica: um e-mail = uma conta.
    // Sem isso, seria possível cadastrar o mesmo e-mail duas vezes, quebrando o login.
    // length = 150: e-mails podem ser longos (ex: nome.sobrenome@empresa.com.br).
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // A senha é armazenada como String, mas NUNCA em texto puro.
    // O AuthService usa passwordEncoder.encode() para transformar "123456"
    // em algo como "$2a$10$..." (hash BCrypt) antes de salvar aqui.
    // Sem length específico porque hashes BCrypt têm tamanho fixo de ~60 caracteres,
    // mas não precisamos limitar.
    @Column(nullable = false)
    private String senha;

    // BigDecimal com precision=10 e scale=2:
    // → precision=10: total de dígitos significativos (incluindo decimais).
    // → scale=2: quantidade de dígitos após a vírgula.
    // Isso permite valores de até R$ 99.999.999,99 — mais que suficiente para salários.
    // No banco PostgreSQL, isso gera o tipo NUMERIC(10, 2).
    // Inicializamos com BigDecimal.ZERO para que novos usuários partam com salário zero,
    // evitando NullPointerException nos cálculos financeiros antes do usuário configurar o salário.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal salario = BigDecimal.ZERO;

    // @Enumerated(EnumType.STRING): instrui o JPA a armazenar o NOME do enum como String no banco.
    // Sem esta anotação, o JPA usaria EnumType.ORDINAL por padrão, salvando 0 para USER e 1 para ADMIN.
    //
    // POR QUE STRING É MELHOR QUE ORDINAL?
    // → Com ORDINAL, se você adicionar um novo papel entre USER e ADMIN (ex: MODERADOR),
    //   todos os valores já gravados no banco ficam errados! O que era ADMIN (índice 1)
    //   passaria a ser MODERADOR, corrompendo dados de produção.
    // → Com STRING, o banco armazena "USER" ou "ADMIN" — texto explícito e estável.
    //   Adicionar novos valores ao enum não afeta os dados existentes.
    //
    // length = 20: garante espaço suficiente para os nomes dos papéis no banco.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    // Inicializamos com Role.USER porque todo novo cadastro deve ser um usuário comum.
    // Assim, mesmo se o código do AuthService esquecer de setar a role,
    // o padrão seguro (usuário comum, sem privilégios) é aplicado automaticamente.
    private Role role = Role.USER;

    // name = "created_at": mapeia para a coluna "created_at" no banco (convenção snake_case para SQL).
    // updatable = false: CRUCIAL! Impede que o JPA atualize este campo em operações UPDATE.
    // Isso garante que a data de criação seja registrada UMA VEZ e nunca mais alterada.
    // Sem updatable = false, qualquer chamada a usuarioRepository.save(usuario) poderia
    // sobrescrever a data de criação original — um bug grave e silencioso.
    @Column(name = "created_at", nullable = false, updatable = false)
    // LocalDateTime.now() é executado no momento em que o objeto é instanciado,
    // capturando automaticamente o momento exato do cadastro.
    private LocalDateTime createdAt = LocalDateTime.now();

    // @OneToMany: define o lado "Um" do relacionamento "Um para Muitos".
    // Um usuário pode ter MUITAS assinaturas. Uma assinatura pertence a UM usuário.
    //
    // mappedBy = "usuario": indica que o campo "usuario" na classe Assinatura
    // é o DONO do relacionamento (é lá que fica a foreign key usuario_id).
    // Sem mappedBy, o JPA criaria uma tabela de junção desnecessária no banco.
    //
    // cascade = CascadeType.ALL: propaga todas as operações (PERSIST, MERGE, REMOVE, etc.)
    // do usuário para suas assinaturas.
    // Prático: ao deletar um usuário, todas as assinaturas são deletadas automaticamente.
    // Ao salvar um usuário com assinaturas novas, elas também são salvas.
    //
    // orphanRemoval = true: se uma assinatura for removida da lista (assinaturas.remove(x)),
    // o JPA a deleta do banco automaticamente.
    // Complementa o cascade: garante que assinaturas "órfãs" (sem dono) sejam removidas.
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    // Inicializamos com ArrayList vazio para evitar NullPointerException ao chamar
    // usuario.getAssinaturas() em um usuário recém-criado que ainda não tem assinaturas.
    // Nunca deixe coleções JPA null — sempre inicialize.
    private List<Assinatura> assinaturas = new ArrayList<>();
}
