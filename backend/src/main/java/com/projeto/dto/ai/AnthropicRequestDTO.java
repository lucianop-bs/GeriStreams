package com.projeto.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Corpo da requisição enviada à API da Anthropic (POST /v1/messages).
 */
public record AnthropicRequestDTO(
        String model,

        @JsonProperty("max_tokens")
        int maxTokens,

        String system,

        List<AnthropicMessageDTO> messages,

        boolean stream
) {}
