package com.projeto.service;

import com.projeto.dto.usuario.AtualizarSalarioDTO;
import com.projeto.dto.usuario.UsuarioResponseDTO;
import com.projeto.model.Role;
import com.projeto.model.Usuario;
import com.projeto.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço de Usuário (Business Logic).
 * <p>
 * Concentra toda a lógica de negócio relacionada a usuários:
 * → Buscar perfil do usuário autenticado
 * → Atualizar salário
 * → Listar todos os usuários
 * → Promover usuários para administrador
 * → Recuperar usuário autenticado do contexto de segurança
 * <p>
 * Padrão: Service (BO)
 * → Nunca expõe entidades diretamente, sempre usa DTOs
 * → Toda validação e regra de negócio fica aqui
 */
@Service
public class UsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Busca os dados do perfil do usuário autenticado.
     * <p>
     * Fluxo:
     * 1. Obtém o usuário autenticado do contexto de segurança do Spring
     * 2. Converte a entidade Usuario para DTO UsuarioResponseDTO
     * 3. Retorna o DTO (que não expõe a senha!)
     * <p>
     * Por que usar DTO?
     * → UsuarioResponseDTO não inclui a senha (por segurança)
     * → Controla exatamente quais dados são enviados ao frontend
     * → Desacopla a estrutura do banco da resposta da API
     *
     * @return DTO com dados do usuário autenticado
     */
    public UsuarioResponseDTO buscarPerfil() {
        logger.info("Consultando perfil do usuário autenticado");
        return UsuarioResponseDTO.fromEntity(getUsuarioAutenticado());
    }

    /**
     * Atualiza o salário do usuário autenticado.
     *
     * @param dto Objeto contendo o novo salário
     * @return DTO do usuário com salário atualizado
     * @Transactional garante que a operação é atômica:
     * → Se algo der errado, toda a transação é desfeita (rollback)
     * → Se tudo der certo, as mudanças são confirmadas (commit)
     * <p>
     * Fluxo:
     * 1. Recupera o usuário autenticado
     * 2. Atualiza o campo salário
     * 3. Salva no banco (JPA detecta a alteração)
     * 4. Retorna os dados atualizados em DTO
     */
    @Transactional
    public UsuarioResponseDTO atualizarSalario(AtualizarSalarioDTO dto) {
        Usuario usuario = getUsuarioAutenticado();
        logger.info("Atualizando salário do usuário {} para {}", usuario.getEmail(), dto.salario());
        usuario.setSalario(dto.salario());
        // save() atualiza no banco porque o usuário já existe (tem ID)
        return UsuarioResponseDTO.fromEntity(usuarioRepository.save(usuario));
    }

    /**
     * Lista TODOS os usuários cadastrados.
     * <p>
     * ATENÇÃO: Esta é uma operação administrativa!
     * → Deve ser protegida por @PreAuthorize("hasRole('ADMIN')") no Controller
     * → Nunca exponha este método a usuários comuns
     * <p>
     * Fluxo:
     * 1. Busca todos os usuários no banco
     * 2. Converte cada um para DTO
     * 3. Retorna como lista
     * <p>
     * Streams em Java (novo em Java 8+):
     * → .stream(): converte List em Stream (pipeline de operações)
     * → .map(): transforma cada elemento de uma forma (Usuario → UsuarioResponseDTO)
     * → .toList(): converte Stream de volta para List
     *
     * @return Lista de DTOs de todos os usuários
     */
    public List<UsuarioResponseDTO> listarTodos() {
        logger.info("Listando todos os usuários cadastrados (operação admin)");
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponseDTO::fromEntity)  // Converte cada Usuario para DTO
                .toList();
    }

    /**
     * Busca um usuário específico pelo ID.
     * <p>
     * Fluxo:
     * 1. Consulta no banco pelo ID
     * 2. Se encontrar, converte para DTO
     * 3. Se não encontrar, lança exceção
     * <p>
     * Optional em Java:
     * → Método que pode ou não retornar um valor
     * → .map(): se tiver valor, aplica a função (Entity → DTO)
     * → .orElseThrow(): se não tiver valor, lança exceção
     *
     * @param id ID do usuário a buscar
     * @return DTO do usuário encontrado
     * @throws IllegalArgumentException se usuário não encontrado
     */
    public UsuarioResponseDTO buscarPorId(Long id) {
        logger.info("Buscando usuário por ID: {}", id);
        return usuarioRepository.findById(id)
                .map(UsuarioResponseDTO::fromEntity)
                .orElseThrow(() -> {
                    logger.warn("Usuário não encontrado com ID: {}", id);
                    return new IllegalArgumentException("Usuário não encontrado.");
                });
    }

    /**
     * Promove um usuário comum para administrador.
     * <p>
     * Regras de negócio implementadas:
     * 1. Um usuário NÃO pode se promover a si mesmo (segurança)
     * 2. Um usuário que já é ADMIN não pode ser promovido novamente
     *
     * @param id ID do usuário a ser promovido
     * @return DTO do usuário com role atualizada para ADMIN
     * @throws IllegalArgumentException se regras de negócio forem violadas
     * @Transactional garante atomicidade da operação
     * <p>
     * SecurityContextHolder: O "cofre" do Spring Security
     * → Armazena informações do usuário autenticado
     * → .getContext().getAuthentication().getName(): recupera o email do usuário autenticado
     * <p>
     * Fluxo:
     * 1. Obtém o email do usuário autenticado
     * 2. Busca o usuário alvo pelo ID
     * 3. Valida que não é auto-promoção
     * 4. Valida que não é dupla promoção
     * 5. Define role como ADMIN
     * 6. Salva no banco
     * 7. Retorna DTO atualizado
     */
    @Transactional
    public UsuarioResponseDTO promoverParaAdmin(Long id) {
        // Obter email do usuário autenticado (aquele que fez a requisição)
        String emailAutenticado = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Admin {} solicitou promoção do usuário ID: {}", emailAutenticado, id);

        // Buscar o usuário alvo (aquele que será promovido)
        Usuario alvo = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Promoção falhou: usuário ID {} não encontrado", id);
                    return new IllegalArgumentException("Usuário não encontrado.");
                });

        // Validação 1: Evitar auto-promoção (segurança: ninguém pode se promover sozinho)
        if (alvo.getEmail().equals(emailAutenticado)) {
            logger.warn("Promoção rejeitada: admin {} tentou se auto-promover", emailAutenticado);
            throw new IllegalArgumentException("Você não pode alterar sua própria role.");
        }

        // Validação 2: Evitar dupla promoção (regra de negócio)
        if (alvo.getRole() == Role.ADMIN) {
            logger.warn("Promoção rejeitada: usuário {} já é ADMIN", alvo.getEmail());
            throw new IllegalArgumentException("Usuário já é administrador.");
        }

        // Alterar role para ADMIN
        alvo.setRole(Role.ADMIN);

        logger.info("Usuário {} promovido para ADMIN por {}", alvo.getEmail(), emailAutenticado);

        // Salvar alteração no banco
        return UsuarioResponseDTO.fromEntity(usuarioRepository.save(alvo));
    }

    /**
     * Método auxiliar privado: recupera o usuário autenticado.
     * <p>
     * Por que privado?
     * → Este é um método utilitário interno, não deve ser chamado pelo Controller
     * → Evita duplicação de código (usado por vários métodos públicos)
     * <p>
     * SecurityContextHolder.getContext():
     * → Obtém o contexto de segurança do thread atual
     * → Contém informações do usuário autenticado
     * <p>
     * .getAuthentication().getName():
     * → Recupera o "name" do Authentication (que é o email neste projeto)
     * <p>
     * Fluxo:
     * 1. Extrai o email do usuário autenticado
     * 2. Busca a entidade completa no banco
     * 3. Se não encontrar, lança IllegalStateException
     *
     * @return Entidade Usuario do usuário autenticado
     * @throws IllegalStateException se usuário não for encontrado (situação anômala)
     */
    public Usuario getUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Usuário autenticado não encontrado no banco: {}", email);
                    return new IllegalStateException("Usuário autenticado não encontrado.");
                });
    }
}
