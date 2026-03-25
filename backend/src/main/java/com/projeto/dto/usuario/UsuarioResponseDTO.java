package com.projeto.dto.usuario;

import com.projeto.model.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        BigDecimal salario,
        String role,
        LocalDateTime createdAt
) {
    public static UsuarioResponseDTO fromEntity(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getSalario(),
                usuario.getRole().name(),
                usuario.getCreatedAt()
        );
    }
}
