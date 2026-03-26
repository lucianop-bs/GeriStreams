package com.projeto.controller;

import com.projeto.dto.auth.JwtResponseDTO;
import com.projeto.dto.auth.LoginRequestDTO;
import com.projeto.dto.auth.RegisterRequestDTO;
import com.projeto.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastrar novo usuário")
    public ResponseEntity<JwtResponseDTO> registrar(@Valid @RequestBody RegisterRequestDTO dto) {
        JwtResponseDTO response = authService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário e obter token JWT")
    public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        JwtResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(response);
    }
}
