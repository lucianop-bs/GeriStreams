package com.projeto.controller;

import com.projeto.dto.usuario.AtualizarSalarioDTO;
import com.projeto.dto.usuario.UsuarioResponseDTO;
import com.projeto.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuário")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna o perfil do usuário autenticado")
    public ResponseEntity<UsuarioResponseDTO> buscarPerfil() {
        return ResponseEntity.ok(usuarioService.buscarPerfil());
    }

    @PutMapping("/me/salario")
    @Operation(summary = "Atualiza o salário do usuário autenticado")
    public ResponseEntity<UsuarioResponseDTO> atualizarSalario(@Valid @RequestBody AtualizarSalarioDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarSalario(dto));
    }
}
