package com.projeto.controller;

import com.projeto.dto.admin.RankingAssinaturaDTO;
import com.projeto.dto.assinatura.AssinaturaResponseDTO;
import com.projeto.dto.usuario.UsuarioResponseDTO;
import com.projeto.service.AssinaturaService;
import com.projeto.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin")
public class AdminController {

    private final UsuarioService usuarioService;
    private final AssinaturaService assinaturaService;

    public AdminController(UsuarioService usuarioService, AssinaturaService assinaturaService) {
        this.usuarioService = usuarioService;
        this.assinaturaService = assinaturaService;
    }

    @GetMapping("/users")
    @Operation(summary = "Lista todos os usuários cadastrados")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Retorna os dados de um usuário específico")
    public ResponseEntity<UsuarioResponseDTO> buscarUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @GetMapping("/users/{id}/subscriptions")
    @Operation(summary = "Lista todas as assinaturas de um usuário específico")
    public ResponseEntity<List<AssinaturaResponseDTO>> listarAssinaturasDoUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(assinaturaService.listarPorUsuario(id));
    }

    /** UC15 — Promover usuário para ADMIN */
    @PatchMapping("/users/{id}/promote")
    @Operation(summary = "Promove um usuário para administrador")
    public ResponseEntity<UsuarioResponseDTO> promoverParaAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.promoverParaAdmin(id));
    }

    /** UC16 — Ranking de serviços mais usados e mais caros */
    @GetMapping("/ranking")
    @Operation(summary = "Ranking dos serviços de assinatura mais usados e com maior gasto total")
    public ResponseEntity<List<RankingAssinaturaDTO>> rankingServicos() {
        return ResponseEntity.ok(assinaturaService.rankingServicos());
    }
}
