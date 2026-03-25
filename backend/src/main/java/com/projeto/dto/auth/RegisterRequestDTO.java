package com.projeto.dto.auth;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record RegisterRequestDTO(

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String nome,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String senha,

        @NotNull(message = "Salário é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "Salário deve ser maior que zero")
        BigDecimal salario
) {}
