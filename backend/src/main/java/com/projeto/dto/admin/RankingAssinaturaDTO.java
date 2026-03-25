package com.projeto.dto.admin;

import java.math.BigDecimal;

/**
 * Dados agregados de um serviço de assinatura para o ranking administrativo.
 */
public record RankingAssinaturaDTO(
        String nomeServico,
        long totalAssinantes,
        BigDecimal valorTotalMensal,
        BigDecimal valorMedio
) {}
