package com.projeto.controller;

import com.projeto.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Relatórios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/pdf")
    @Operation(summary = "Exporta o relatório financeiro do usuário em PDF")
    public ResponseEntity<byte[]> exportarPdf() {
        byte[] pdf = relatorioService.gerarRelatorioPdf();

        String nomeArquivo = "relatorio-geristreams-" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
