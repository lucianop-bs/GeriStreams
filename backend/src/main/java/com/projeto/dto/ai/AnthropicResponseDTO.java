package com.projeto.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Resposta da API da Anthropic (não-streaming).
 */
public record AnthropicResponseDTO(
        String id,
        List<AnthropicContentBlockDTO> content,

        @JsonProperty("stop_reason")
        String stopReason,

        String model
) {
    /** Extrai o texto do primeiro bloco de conteúdo do tipo "text". */
    public String extractText() {
        if (content == null) return "";
        return content.stream()
                .filter(b -> "text".equals(b.type()))
                .map(AnthropicContentBlockDTO::text)
                .findFirst()
                .orElse("");
    }
}
