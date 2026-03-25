package com.projeto.service;

import com.projeto.dto.usuario.AtualizarSalarioDTO;
import com.projeto.dto.usuario.UsuarioResponseDTO;
import com.projeto.model.Role;
import com.projeto.model.Usuario;
import com.projeto.repository.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public UsuarioResponseDTO buscarPerfil() {
        return UsuarioResponseDTO.fromEntity(getUsuarioAutenticado());
    }

    @Transactional
    public UsuarioResponseDTO atualizarSalario(AtualizarSalarioDTO dto) {
        Usuario usuario = getUsuarioAutenticado();
        usuario.setSalario(dto.salario());
        return UsuarioResponseDTO.fromEntity(usuarioRepository.save(usuario));
    }

    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponseDTO::fromEntity)
                .toList();
    }

    public UsuarioResponseDTO buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponseDTO::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }

    @Transactional
    public UsuarioResponseDTO promoverParaAdmin(Long id) {
        String emailAutenticado = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario alvo = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (alvo.getEmail().equals(emailAutenticado)) {
            throw new IllegalArgumentException("Você não pode alterar sua própria role.");
        }
        if (alvo.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Usuário já é administrador.");
        }

        alvo.setRole(Role.ADMIN);
        return UsuarioResponseDTO.fromEntity(usuarioRepository.save(alvo));
    }

    public Usuario getUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
    }
}
