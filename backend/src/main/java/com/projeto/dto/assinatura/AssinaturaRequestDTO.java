package com.projeto.dto.assinatura;

import com.projeto.model.CategoriaAssinatura;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AssinaturaRequestDTO(

        @NotBlank(message = "Nome do serviço é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "Categoria é obrigatória")
        CategoriaAssinatura categoria
) {}
