package com.projeto.controller;

import com.projeto.dto.ai.AiDicasResponseDTO;
import com.projeto.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Inteligência Artificial")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/dicas")
    @Operation(summary = "Gera dicas personalizadas de economia via Claude AI com base no perfil financeiro do usuário")
    public ResponseEntity<AiDicasResponseDTO> gerarDicas() {
        return ResponseEntity.ok(aiService.gerarDicas());
    }
}
