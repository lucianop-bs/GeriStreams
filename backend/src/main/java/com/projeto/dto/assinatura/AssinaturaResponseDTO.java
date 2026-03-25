package com.projeto.dto.assinatura;

import com.projeto.model.Assinatura;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssinaturaResponseDTO(
        Long id,
        String nome,
        BigDecimal valor,
        String categoria,
        Boolean ativo,
        LocalDateTime createdAt
) {
    public static AssinaturaResponseDTO fromEntity(Assinatura assinatura) {
        return new AssinaturaResponseDTO(
                assinatura.getId(),
                assinatura.getNome(),
                assinatura.getValor(),
                assinatura.getCategoria().name(),
                assinatura.getAtivo(),
                assinatura.getCreatedAt()
        );
    }
}
