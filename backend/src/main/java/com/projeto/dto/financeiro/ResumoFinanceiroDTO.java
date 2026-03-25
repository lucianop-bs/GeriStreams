package com.projeto.dto.financeiro;

import com.projeto.dto.assinatura.AssinaturaResponseDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ResumoFinanceiroDTO(
        BigDecimal salario,
        BigDecimal totalMensal,
        BigDecimal percentualDoSalario,
        List<AssinaturaResponseDTO> assinaturas,
        Map<String, BigDecimal> gastosPorCategoria
) {}
