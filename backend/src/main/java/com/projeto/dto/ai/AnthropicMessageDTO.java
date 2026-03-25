package com.projeto.dto.ai;

/**
 * Representa uma mensagem no formato esperado pela API da Anthropic.
 */
public record AnthropicMessageDTO(String role, String content) {}
