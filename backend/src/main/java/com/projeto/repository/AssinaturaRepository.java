package com.projeto.repository;

import com.projeto.model.Assinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository (DAO) de Assinatura.
 * <p>
 * Interface que define acesso a Assinatura no banco de dados.
 * Spring Data JPA implementa todos os métodos automaticamente.
 * <p>
 * Combina:
 * 1. Métodos simples gerados automaticamente por Spring Data (find, exists, etc)
 * 2. Métodos complexos com @Query customizadas (queries em JPQL ou SQL nativo)
 * <p>
 * O que é JPQL?
 * → Java Persistence Query Language
 * → Similar a SQL, mas trabalha com objetos, não tabelas
 * → @Query("SELECT a FROM Assinatura a WHERE ...") — "a" é um alias para Assinatura
 * → Spring/JPA traduz automaticamente para SQL
 * <p>
 * Vantagem de JPQL vs SQL nativo:
 * → Agnóstico do banco de dados (PostgreSQL, MySQL, etc)
 * → O JPA cuida da tradução automática
 * → Se mudar de banco, não precisa alterar as queries
 */
public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {

    /**
     * Lista TODAS as assinaturas de um usuário (ativas e inativas).
     * <p>
     * Query gerada automaticamente:
     * → SELECT * FROM assinaturas WHERE usuario_id = ?
     * <p>
     * Padrão Spring Data:
     * → findBy[Atributo]([valor]) gera query automaticamente
     * → O "Id" é mapeado para "id" do Usuario
     * → O "Usuario" é mapeado para a coluna "usuario_id"
     *
     * @param usuarioId ID do usuário
     * @return Lista com todas as assinaturas daquele usuário (status irrelevante)
     */
    List<Assinatura> findByUsuarioId(Long usuarioId);

    /**
     * Lista apenas as assinaturas ATIVAS de um usuário.
     * <p>
     * Query gerada automaticamente:
     * → SELECT * FROM assinaturas WHERE usuario_id = ? AND ativo = true
     * <p>
     * Padrão Spring Data:
     * → And na query = AND no SQL
     * → True no método = true no banco (BOOLEAN em SQL)
     * <p>
     * Utilidade:
     * → Cálculos financeiros incluem apenas assinaturas ativas
     * → Assinaturas pausadas (ativo=false) não contam na despesa
     *
     * @param usuarioId ID do usuário
     * @return Lista com assinaturas ativas
     */
    List<Assinatura> findByUsuarioIdAndAtivoTrue(Long usuarioId);

    /**
     * Calcula a SOMA dos valores de assinaturas ativas.
     *
     * @param usuarioId ID do usuário
     * @return Soma dos valores, ou null se sem assinaturas
     * @Query("SELECT SUM(a.valor) FROM Assinatura a WHERE a.usuario.id = :usuarioId AND a.ativo = true")
     * <p>
     * O que acontece aqui?
     * → SELECT SUM(a.valor): soma todos os valores (a.valor é BigDecimal)
     * → FROM Assinatura a: da tabela de Assinatura (a é o alias)
     * → WHERE a.usuario.id = :usuarioId: relacionamento JPA (a.usuario acessa o Usuario relacionado)
     * → AND a.ativo = true: filtra apenas as ativas
     * → :usuarioId é um parameter named binding (seguro contra SQL injection)
     * <p>
     * Retorna: BigDecimal (BigDecimal é obrigatório para dinheiro!)
     * Retorna null se não há registros (precisa verificar no código)
     * <p>
     * SQL equivalente:
     * → SELECT SUM(valor) FROM assinaturas WHERE usuario_id = ? AND ativo = true
     * <p>
     * Utilidade:
     * → Calcular despesa mensal total do usuário
     * → Base para calcular percentual do salário gasto
     */
    @Query("SELECT SUM(a.valor) FROM Assinatura a WHERE a.usuario.id = :usuarioId AND a.ativo = true")
    BigDecimal sumValorAtivoByUsuarioId(Long usuarioId);

    /**
     * Agrupa gastos por categoria e calcula a soma para cada uma.
     *
     * @param usuarioId ID do usuário
     * @return Lista de arrays [categoria, soma]
     * @Query("SELECT a.categoria, SUM(a.valor) FROM Assinatura a WHERE a.usuario.id = :usuarioId AND a.ativo = true GROUP BY a.categoria")
     * <p>
     * O que retorna?
     * → List<Object[]> onde cada elemento é um array [categoria, soma]
     * → row[0] = CategoriaAssinatura enum
     * → row[1] = BigDecimal (soma)
     * <p>
     * GROUP BY:
     * → Agrupa registros pela categoria
     * → Calcula SUM para cada grupo
     * <p>
     * SQL equivalente:
     * → SELECT categoria, SUM(valor) FROM assinaturas
     * WHERE usuario_id = ? AND ativo = true
     * GROUP BY categoria
     * <p>
     * Utilidade:
     * → Mostrar no frontend quanto gasta em cada categoria
     * → Criar gráficos de distribuição de gastos
     * → Exemplo: Streaming Video: R$ 250 | Música: R$ 80 | Software: R$ 150
     */
    @Query("SELECT a.categoria, SUM(a.valor) FROM Assinatura a WHERE a.usuario.id = :usuarioId AND a.ativo = true GROUP BY a.categoria")
    List<Object[]> sumValorGroupedByCategoriaAndUsuarioId(Long usuarioId);

    /**
     * Ranking de USUÁRIOS com maior gasto total.
     *
     * @return Lista de arrays [usuarioId, soma], ordenado descendente
     * @Query("SELECT a.usuario.id, SUM(a.valor) FROM Assinatura a WHERE a.ativo = true GROUP BY a.usuario.id ORDER BY SUM(a.valor) DESC")
     * <p>
     * O que retorna?
     * → List<Object[]> onde cada elemento é um array [usuarioId, soma]
     * → row[0] = Long (ID do usuário)
     * → row[1] = BigDecimal (gasto total)
     * → Ordenado do maior para o menor gasto
     * <p>
     * GROUP BY a.usuario.id:
     * → Agrupa por usuário (não por assinatura individual)
     * → Calcula SUM para cada usuário
     * <p>
     * ORDER BY ... DESC:
     * → Descendente = do maior para o menor
     * → Usuário com mais gasto aparece primeiro
     * <p>
     * SQL equivalente:
     * → SELECT usuario_id, SUM(valor) FROM assinaturas
     * WHERE ativo = true
     * GROUP BY usuario_id
     * ORDER BY SUM(valor) DESC
     * <p>
     * Utilidade:
     * → Dashboard administrativo
     * → Ver quais usuários gastam mais
     * → Identificar power users (usuários com muitas assinaturas)
     */
    @Query("SELECT a.usuario.id, SUM(a.valor) FROM Assinatura a WHERE a.ativo = true GROUP BY a.usuario.id ORDER BY SUM(a.valor) DESC")
    List<Object[]> rankingGastosPorUsuario();

    /**
     * Ranking de SERVIÇOS (nomes de assinatura) mais populares.
     *
     * @return Lista de arrays [nome, contagem, soma, média], ordenado descendente
     * @Query(""" SELECT a.nome, COUNT(a), SUM(a.valor), AVG(a.valor)
     * FROM Assinatura a
     * WHERE a.ativo = true
     * GROUP BY a.nome
     * ORDER BY SUM(a.valor) DESC
     * """)
     * <p>
     * Tripla aspas ("""):
     * → Permite string multilinhas em Java 15+
     * → Sem isto, seria uma linha gigantesca
     * → Melhor legibilidade
     * <p>
     * O que retorna?
     * → List<Object[]> onde cada elemento é um array [nome, contagem, soma, média]
     * → row[0] = String (nome do serviço)
     * → row[1] = Long (número de usuários que assinam)
     * → row[2] = BigDecimal (soma total gasto por todos)
     * → row[3] = BigDecimal (média de valor por assinatura)
     * <p>
     * Estatísticas:
     * → COUNT(a): quantas assinaturas daquele nome
     * → SUM(a.valor): quanto foi gasto no total
     * → AVG(a.valor): qual a média de valor
     * <p>
     * GROUP BY a.nome:
     * → Agrupa por nome (Netflix, Spotify, etc)
     * → Calcula agregações para cada serviço
     * <p>
     * ORDER BY SUM(a.valor) DESC:
     * → Serviço com mais gasto total aparece primeiro
     * → Descente = maior para menor
     * <p>
     * SQL equivalente:
     * → SELECT nome, COUNT(*), SUM(valor), AVG(valor) FROM assinaturas
     * WHERE ativo = true
     * GROUP BY nome
     * ORDER BY SUM(valor) DESC
     * <p>
     * Exemplo de resultado:
     * → Netflix: 250 usuários | Total: R$ 12.500 | Média: R$ 50
     * → Spotify: 300 usuários | Total: R$ 8.400 | Média: R$ 28
     * → Disney+: 180 usuários | Total: R$ 5.400 | Média: R$ 30
     * <p>
     * Utilidade:
     * → Dashboard de estatísticas globais
     * → Saber qual é o serviço mais popular
     * → Análise de mercado/tendências
     * → Informação para marketing/análise
     */
    @Query("""
            SELECT a.nome, COUNT(a), SUM(a.valor), AVG(a.valor)
            FROM Assinatura a
            WHERE a.ativo = true
            GROUP BY a.nome
            ORDER BY SUM(a.valor) DESC
            """)
    List<Object[]> rankingServicos();
}
