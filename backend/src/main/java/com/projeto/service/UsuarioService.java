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

@Service
public class UsuarioService {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public UsuarioResponseDTO buscarPerfil() {
        logger.info("Consultando perfil do usuário autenticado");
        return UsuarioResponseDTO.fromEntity(getUsuarioAutenticado());
    }

    @Transactional
    public UsuarioResponseDTO atualizarSalario(AtualizarSalarioDTO dto) {
        Usuario usuario = getUsuarioAutenticado();
        logger.info("Atualizando salário do usuário {} para {}", usuario.getEmail(), dto.salario());
        usuario.setSalario(dto.salario());
        return UsuarioResponseDTO.fromEntity(usuarioRepository.save(usuario));
    }

    public List<UsuarioResponseDTO> listarTodos() {
        logger.info("Listando todos os usuários cadastrados (operação admin)");
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponseDTO::fromEntity)
                .toList();
    }

    public UsuarioResponseDTO buscarPorId(Long id) {
        logger.info("Buscando usuário por ID: {}", id);
        return usuarioRepository.findById(id)
                .map(UsuarioResponseDTO::fromEntity)
                .orElseThrow(() -> {
                    logger.warn("Usuário não encontrado com ID: {}", id);
                    return new IllegalArgumentException("Usuário não encontrado.");
                });
    }

    @Transactional
    public UsuarioResponseDTO promoverParaAdmin(Long id) {
        String emailAutenticado = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Admin {} solicitou promoção do usuário ID: {}", emailAutenticado, id);

        Usuario alvo = usuarioRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Promoção falhou: usuário ID {} não encontrado", id);
                    return new IllegalArgumentException("Usuário não encontrado.");
                });

        if (alvo.getEmail().equals(emailAutenticado)) {
            logger.warn("Promoção rejeitada: admin {} tentou se auto-promover", emailAutenticado);
            throw new IllegalArgumentException("Você não pode alterar sua própria role.");
        }

        if (alvo.getRole() == Role.ADMIN) {
            logger.warn("Promoção rejeitada: usuário {} já é ADMIN", alvo.getEmail());
            throw new IllegalArgumentException("Usuário já é administrador.");
        }

        alvo.setRole(Role.ADMIN);

        logger.info("Usuário {} promovido para ADMIN por {}", alvo.getEmail(), emailAutenticado);

        return UsuarioResponseDTO.fromEntity(usuarioRepository.save(alvo));
    }

    public Usuario getUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Usuário autenticado não encontrado no banco: {}", email);
                    return new IllegalStateException("Usuário autenticado não encontrado.");
                });
    }
}
