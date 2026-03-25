package com.projeto.dto.auth;

public record JwtResponseDTO(
        String token,
        String tipo,
        String email,
        String role
) {
    public JwtResponseDTO(String token, String email, String role) {
        this(token, "Bearer", email, role);
    }
}
