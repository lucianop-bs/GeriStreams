package com.projeto.controller;

import com.projeto.dto.assinatura.AssinaturaRequestDTO;
import com.projeto.dto.assinatura.AssinaturaResponseDTO;
import com.projeto.dto.financeiro.ResumoFinanceiroDTO;
import com.projeto.service.AssinaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Assinaturas")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    public AssinaturaController(AssinaturaService assinaturaService) {
        this.assinaturaService = assinaturaService;
    }

    @GetMapping
    @Operation(summary = "Lista todas as assinaturas do usuário autenticado")
    public ResponseEntity<List<AssinaturaResponseDTO>> listar() {
        return ResponseEntity.ok(assinaturaService.listar());
    }

    @PostMapping
    @Operation(summary = "Adiciona uma nova assinatura")
    public ResponseEntity<AssinaturaResponseDTO> criar(@Valid @RequestBody AssinaturaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assinaturaService.criar(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma assinatura existente")
    public ResponseEntity<AssinaturaResponseDTO> atualizar(@PathVariable Long id,
                                                           @Valid @RequestBody AssinaturaRequestDTO dto) {
        return ResponseEntity.ok(assinaturaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma assinatura")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        assinaturaService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Ativa ou desativa uma assinatura")
    public ResponseEntity<AssinaturaResponseDTO> alternarAtivo(@PathVariable Long id) {
        return ResponseEntity.ok(assinaturaService.alternarAtivo(id));
    }

    @GetMapping("/resumo")
    @Operation(summary = "Retorna o resumo financeiro com cálculo de gastos em relação ao salário")
    public ResponseEntity<ResumoFinanceiroDTO> resumoFinanceiro() {
        return ResponseEntity.ok(assinaturaService.calcularResumoFinanceiro());
    }
}
