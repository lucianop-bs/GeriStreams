package com.projeto.dto.usuario;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AtualizarSalarioDTO(

        @NotNull(message = "Salário é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "Salário deve ser maior que zero")
        BigDecimal salario
) {}
