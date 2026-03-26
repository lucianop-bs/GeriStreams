package com.projeto.service;

import com.projeto.dto.admin.RankingAssinaturaDTO;
import com.projeto.dto.assinatura.AssinaturaRequestDTO;
import com.projeto.dto.assinatura.AssinaturaResponseDTO;
import com.projeto.dto.financeiro.ResumoFinanceiroDTO;
import com.projeto.model.Assinatura;
import com.projeto.model.CategoriaAssinatura;
import com.projeto.model.Usuario;
import com.projeto.repository.AssinaturaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de Assinatura (Business Logic).
 * <p>
 * Concentra toda a lógica de negócio relacionada a assinaturas:
 * → CRUD básico (criar, ler, atualizar, deletar)
 * → Alternar status ativo/inativo
 * → Calcular resumo financeiro (gasto total, percentual do salário, gastos por categoria)
 * → Gerar rankings (serviços mais populares)
 * <p>
 * Princípio fundamental: isolamento por usuário
 * → Cada usuário só pode ver/editar suas PRÓPRIAS assinaturas
 * → Um usuário comum (USER) NUNCA pode acessar assinaturas de outro usuário
 * → Apenas admins podem visualizar assinaturas de outros usuários
 * <p>
 * Padrão: Service (BO)
 */
@Service
public class AssinaturaService {

    private static final Logger logger = LoggerFactory.getLogger(AssinaturaService.class);

    private final AssinaturaRepository assinaturaRepository;
    private final UsuarioService usuarioService;

    public AssinaturaService(AssinaturaRepository assinaturaRepository, UsuarioService usuarioService) {
        this.assinaturaRepository = assinaturaRepository;
        this.usuarioService = usuarioService;
    }

    /**
     * Lista TODAS as assinaturas do usuário autenticado.
     * <p>
     * Segurança:
     * → Só retorna assinaturas daquele usuário
     * → Impossível ver assinaturas de outro usuário (a query filtra por usuarioId)
     * <p>
     * Fluxo:
     * 1. Obtém o usuário autenticado
     * 2. Busca todas as assinaturas daquele usuário
     * 3. Converte cada uma para DTO (não expõe a entidade JPA)
     * 4. Retorna como lista
     *
     * @return Lista de assinaturas do usuário
     */
    public List<AssinaturaResponseDTO> listar() {
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        return assinaturaRepository.findByUsuarioId(usuario.getId()).stream()
                .map(AssinaturaResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Cria uma nova assinatura para o usuário autenticado.
     *
     * @param dto Dados da assinatura a ser criada
     * @return DTO da assinatura criada (com ID gerado pelo banco)
     * @Transactional garante que:
     * → A assinatura é criada atomicamente (tudo ou nada)
     * → Se houver erro, é desfeita automaticamente
     * <p>
     * Fluxo:
     * 1. Recupera o usuário autenticado
     * 2. Cria nova entidade Assinatura (vazia)
     * 3. Popula os campos com dados do DTO
     * 4. Associa a assinatura ao usuário (relacionamento)
     * 5. Salva no banco (JPA gera o ID automaticamente)
     * 6. Retorna o DTO com todos os dados (incluindo ID gerado)
     * <p>
     * Por que DTO de entrada?
     * → O frontend envia AssinaturaRequestDTO (apenas nome, valor, categoria)
     * → Não permite que o frontend defina usuarioId, id, createdAt, etc
     * → Segurança: evita ataques de parameter pollution
     */
    @Transactional
    public AssinaturaResponseDTO criar(AssinaturaRequestDTO dto) {
        // Obtém o usuário autenticado
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        logger.info("Criando assinatura '{}' para o usuário: {}", dto.nome(), usuario.getEmail());

        // Instancia uma nova assinatura (ainda não existe no banco)
        Assinatura assinatura = new Assinatura();

        // Popula os campos com os dados do DTO
        assinatura.setNome(dto.nome());
        assinatura.setValor(dto.valor());
        assinatura.setCategoria(dto.categoria());

        // Associa a assinatura ao usuário autenticado
        // Isso garante que a assinatura sempre pertence ao usuário que a criou
        assinatura.setUsuario(usuario);

        // Salva no banco. Spring Data JPA:
        // 1. Insere um novo registro na tabela assinaturas
        // 2. Gera automaticamente o ID (IDENTITY)
        // 3. Retorna a entidade com o ID preenchido
        return AssinaturaResponseDTO.fromEntity(assinaturaRepository.save(assinatura));
    }

    /**
     * Atualiza uma assinatura existente.
     * <p>
     * Segurança:
     * → Só permite atualizar assinaturas do próprio usuário
     * → Usa buscarPorIdDoUsuario() para validar propriedade
     * <p>
     * Fluxo:
     * 1. Busca a assinatura verificando que pertence ao usuário
     * 2. Atualiza os campos (nome, valor, categoria)
     * 3. Salva as mudanças no banco
     * 4. Retorna DTO atualizado
     * <p>
     * Por que .save() atualiza em vez de inserir?
     * → Se a entidade já tem ID (@Id preenchido), save() faz UPDATE
     * → Se a entidade não tem ID, save() faz INSERT
     * → O JPA detecta automaticamente qual operação fazer
     *
     * @param id  ID da assinatura a atualizar
     * @param dto Novos dados (nome, valor, categoria)
     * @return DTO da assinatura atualizada
     * @throws IllegalArgumentException se assinatura não encontrada ou não pertence ao usuário
     */
    @Transactional
    public AssinaturaResponseDTO atualizar(Long id, AssinaturaRequestDTO dto) {
        logger.info("Atualizando assinatura ID: {}", id);
        // Busca a assinatura verificando que pertence ao usuário
        Assinatura assinatura = buscarPorIdDoUsuario(id);

        // Atualiza os campos
        assinatura.setNome(dto.nome());
        assinatura.setValor(dto.valor());
        assinatura.setCategoria(dto.categoria());

        // Salva as mudanças no banco
        return AssinaturaResponseDTO.fromEntity(assinaturaRepository.save(assinatura));
    }

    /**
     * Deleta uma assinatura do usuário autenticado.
     * <p>
     * Fluxo:
     * 1. Busca a assinatura verificando propriedade
     * 2. Deleta do banco
     * <p>
     * Cascata de deleção:
     * → Não precisa deletar manualmente das outras tabelas
     * → O @OneToMany(cascade = CascadeType.ALL) no Usuario cuida disso
     * → Se um usuário é deletado, todas suas assinaturas também são
     *
     * @param id ID da assinatura a deletar
     * @throws IllegalArgumentException se assinatura não encontrada ou não pertence ao usuário
     */
    @Transactional
    public void remover(Long id) {
        logger.info("Removendo assinatura ID: {}", id);
        Assinatura assinatura = buscarPorIdDoUsuario(id);
        assinaturaRepository.delete(assinatura);
        logger.info("Assinatura ID: {} removida com sucesso", id);
    }

    /**
     * Alterna o status ativo/inativo de uma assinatura.
     * <p>
     * Regra de negócio:
     * → Assinaturas inativas não são contadas no resumo financeiro
     * → Útil para "pausar" uma assinatura sem deletá-la
     * <p>
     * Lógica:
     * → Se estava ativo (true), desativa (false)
     * → Se estava inativo (false), ativa (true)
     * → !assinatura.getAtivo() inverte o booleano
     *
     * @param id ID da assinatura a alternar
     * @return DTO da assinatura com status atualizado
     */
    @Transactional
    public AssinaturaResponseDTO alternarAtivo(Long id) {
        Assinatura assinatura = buscarPorIdDoUsuario(id);
        boolean novoStatus = !assinatura.getAtivo();
        assinatura.setAtivo(novoStatus);
        logger.info("Assinatura ID: {} alterada para {}", id, novoStatus ? "ATIVA" : "INATIVA");
        return AssinaturaResponseDTO.fromEntity(assinaturaRepository.save(assinatura));
    }

    /**
     * Calcula um resumo financeiro completo do usuário.
     * <p>
     * Retorna:
     * → Salário do usuário
     * → Gasto total com assinaturas ativas
     * → Percentual do salário gasto com assinaturas (quanto % do salário vai embora)
     * → Lista de todas as assinaturas ativas
     * → Gastos agrupados por categoria (para gráficos no frontend)
     * <p>
     * Fluxo:
     * 1. Obtém o usuário autenticado
     * 2. Busca APENAS assinaturas ativas (ativo = true)
     * 3. Calcula o SOMA dos valores ativos
     * 4. Calcula o percentual (total / salário * 100)
     * 5. Agrupa gastos por categoria
     * 6. Retorna tudo em um DTO estruturado
     * <p>
     * Lógica de cálculo do percentual:
     * → Se salário é zero: retorna 0% (não dá divisão por zero)
     * → Se salário > 0: percentual = (total / salário) × 100
     * → Usa HALF_UP para arredondar corretamente (ex: 33.554% → 33.55%)
     * <p>
     * Por que usar null check em total?
     * → Se o usuário não tem assinaturas, a query SUM retorna null (não 0)
     * → Precisamos garantir que total é um BigDecimal válido
     *
     * @return DTO com resumo financeiro completo
     */
    public ResumoFinanceiroDTO calcularResumoFinanceiro() {
        // Obtém o usuário autenticado
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        Long usuarioId = usuario.getId();

        // Lista todas as assinaturas ATIVAS do usuário
        List<AssinaturaResponseDTO> assinaturas = assinaturaRepository
                .findByUsuarioIdAndAtivoTrue(usuarioId)  // Query customizada no Repository
                .stream()
                .map(AssinaturaResponseDTO::fromEntity)
                .toList();

        // Calcula a SOMA dos valores das assinaturas ativas
        BigDecimal total = assinaturaRepository.sumValorAtivoByUsuarioId(usuarioId);

        // Se não há assinaturas, a query retorna null; precisamos converter para ZERO
        if (total == null) total = BigDecimal.ZERO;

        // Obtém o salário do usuário
        BigDecimal salario = usuario.getSalario();

        // Calcula o percentual: (total / salário) × 100
        // Precisa verificar antes se salário é zero (evitar divisão por zero)
        BigDecimal percentual = salario.compareTo(BigDecimal.ZERO) > 0
                // Se salário > 0: calcular percentual
                // .divide(salario, 4, RoundingMode.HALF_UP): divide total por salário com 4 casas decimais
                // .multiply(BigDecimal.valueOf(100)): multiplica por 100 para converter em percentual
                ? total.divide(salario, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                // Se salário = 0: retornar 0%
                : BigDecimal.ZERO;

        // Agrupa gastos por categoria
        // LinkedHashMap mantém a ordem de inserção (importante para previsibilidade)
        Map<String, BigDecimal> gastosPorCategoria = new LinkedHashMap<>();

        // Consulta que retorna [categoria, soma] para cada categoria com assinaturas
        assinaturaRepository.sumValorGroupedByCategoriaAndUsuarioId(usuarioId)
                .forEach(row -> gastosPorCategoria.put(
                        // row[0] é a categoria (CategoriaAssinatura enum)
                        // .name() retorna a string do enum (ex: "STREAMING_VIDEO")
                        ((CategoriaAssinatura) row[0]).name(),
                        // row[1] é a soma dos valores (BigDecimal)
                        (BigDecimal) row[1]
                ));

        // Retorna o DTO com todos os dados agrupados
        return new ResumoFinanceiroDTO(salario, total, percentual, assinaturas, gastosPorCategoria);
    }

    /**
     * Gera um ranking dos serviços mais populares (mais assinados).
     * <p>
     * Retorna informações para cada serviço:
     * → Nome do serviço (ex: "Netflix")
     * → Quantidade de usuários que assinam
     * → Soma total gasto (por todos os usuários)
     * → Média de valor por assinatura
     * <p>
     * Útil para:
     * → Dashboard administrativo
     * → Análise de tendências
     * → Visualização de dados
     * <p>
     * Fluxo:
     * 1. Executa a query rankingServicos() no Repository
     * 2. Converte cada linha de resultado em RankingAssinaturaDTO
     * 3. Retorna como lista
     * <p>
     * Por que Object[]?
     * → @Query customizadas retornam Object[] quando não retornam entidades
     * → row[0]: nome (String)
     * → row[1]: contagem (Long)
     * → row[2]: soma (BigDecimal)
     * → row[3]: média (BigDecimal)
     *
     * @return Lista de DTOs com ranking de serviços
     */
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

    /**
     * Lista assinaturas de um usuário específico.
     * <p>
     * ATENÇÃO: Este método é administrativo!
     * → Deve ser protegido por @PreAuthorize("hasRole('ADMIN')") no Controller
     * → Permite que admins vejam assinaturas de qualquer usuário
     * <p>
     * Fluxo:
     * 1. Busca todas as assinaturas do usuário pelo ID
     * 2. Converte para DTOs
     * 3. Retorna lista
     *
     * @param usuarioId ID do usuário cujas assinaturas serão listadas
     * @return Lista de assinaturas do usuário
     */
    public List<AssinaturaResponseDTO> listarPorUsuario(Long usuarioId) {
        return assinaturaRepository.findByUsuarioId(usuarioId).stream()
                .map(AssinaturaResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Método privado auxiliar: busca uma assinatura verificando propriedade.
     * <p>
     * SEGURANÇA CRÍTICA:
     * → Esta função garante que um usuário só pode acessar suas PRÓPRIAS assinaturas
     * → Toda operação de edição/deleção passa por aqui
     * → Se um usuário tentar acessar assinatura de outro: exceção é lançada
     * <p>
     * Fluxo:
     * 1. Obtém o usuário autenticado (aquele que fez a requisição)
     * 2. Busca a assinatura pelo ID
     * 3. Valida que a assinatura pertence ao usuário (comparando IDs de usuário)
     * 4. Se não pertencer: lança exceção
     * 5. Se pertencer: retorna a assinatura
     * <p>
     * Filter no Stream:
     * → .filter(a -> a.getUsuario().getId().equals(usuario.getId()))
     * → Só permite assinaturas cuja proprietária é o usuário autenticado
     * → Se não passar no filtro, Optional fica vazio
     * → .orElseThrow() lança exceção se Optional vazio
     * <p>
     * Por que isso importa?
     * → Sem esta validação, um usuário poderia fazer:
     * DELETE /api/assinaturas/999 (onde 999 é ID de outra pessoa)
     * → Com este método: a validação impede isso
     *
     * @param id ID da assinatura a buscar
     * @return Entidade Assinatura se pertence ao usuário
     * @throws IllegalArgumentException se não encontrada ou não pertence ao usuário
     */
    private Assinatura buscarPorIdDoUsuario(Long id) {
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        return assinaturaRepository.findById(id)
                // Filter: só passa se a assinatura pertence ao usuário
                .filter(a -> a.getUsuario().getId().equals(usuario.getId()))
                // Se passou no filtro: retorna a assinatura
                // Se não passou: lança exceção
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada."));
    }
}
