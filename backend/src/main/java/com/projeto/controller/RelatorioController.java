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

/**
 * Controller de Relatórios.
 * <p>
 * Gera e exporta relatórios para os usuários.
 * <p>
 * Funcionalidade:
 * → Gerar relatório financeiro em PDF
 * → Incluir resumo de assinaturas
 * → Incluir análise de gastos
 * <p>
 * Acesso: Qualquer usuário autenticado (USER ou ADMIN)
 * <p>
 * Tecnologia:
 * → OpenPDF: biblioteca para gerar PDFs
 * → Dependência no pom.xml: openpdf-1.3.30
 * <p>
 * Fluxo de uso:
 * 1. Usuário clica em "Baixar Relatório" no frontend
 * 2. Frontend faz GET /api/reports/pdf com token JWT
 * 3. Backend chama relatorioService.gerarRelatorioPdf()
 * 4. Service gera PDF em memória (byte[])
 * 5. Controller retorna com headers apropriados
 * 6. Browser baixa arquivo (Content-Disposition: attachment)
 * 7. Arquivo é salvo com data: "relatorio-geristreams-2025-01-15.pdf"
 */
@RestController
@RequestMapping("/api/reports")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Relatórios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    /**
     * Exporta relatório financeiro do usuário em PDF.
     * <p>
     * Rota: GET /api/reports/pdf
     * Autenticação: REQUERIDA (qualquer usuário autenticado)
     * Content-Type: application/pdf (retorna binário)
     * Status: 200 OK
     * <p>
     * Conteúdo do PDF:
     * → Nome do usuário
     * → Email
     * → Data do relatório
     * → Salário mensal
     * → Total gasto com assinaturas
     * → Percentual do salário gasto
     * → Lista de assinaturas ativas
     * → Gasto por categoria (gráfico)
     * <p>
     * Fluxo de resposta HTTP:
     * 1. Service gera PDF em memória
     * 2. Controller adiciona headers HTTP especiais
     * 3. Browser reconhece tipo (application/pdf)
     * 4. Content-Disposition: attachment (faz download, não abre inline)
     * 5. Filename: nome do arquivo para salvar
     * 6. Body: bytes do PDF
     * <p>
     * byte[]:
     * → Array de bytes (cada número 0-255)
     * → Binário do PDF
     * → OpenPDF converte o documento em bytes
     * <p>
     * LocalDate.now():
     * → Data de hoje
     * → Neste momento em que a requisição é feita
     * → DateTimeFormatter converte para string formatada
     * <p>
     * DateTimeFormatter.ofPattern("yyyy-MM-dd"):
     * → "yyyy": 4 dígitos do ano (2025)
     * → "MM": 2 dígitos do mês (01-12)
     * → "dd": 2 dígitos do dia (01-31)
     * → Resultado: "2025-01-15"
     * <p>
     * HttpHeaders.CONTENT_DISPOSITION:
     * → Header HTTP que controla como o navegador trata a resposta
     * → "attachment; filename=\"...\": faz download (não inline)
     * → Sem isto: PDF poderia abrir no navegador em vez de baixar
     * <p>
     * MediaType.APPLICATION_PDF:
     * → Content-Type: application/pdf
     * → Diz ao navegador: isto é um PDF
     * → Navegador sabe como lidar (ou baixar)
     * <p>
     * Exemplo de headers na resposta:
     * → Content-Type: application/pdf
     * → Content-Disposition: attachment; filename="relatorio-geristreams-2025-01-15.pdf"
     * → Content-Length: 12345 (tamanho do PDF)
     * <p>
     * ResponseEntity.ok():
     * → Status 200 OK
     * → Indica sucesso
     * → Cliente pode processar o arquivo
     *
     * @return ResponseEntity com byte array do PDF + headers apropriados
     */
    @GetMapping("/pdf")
    @Operation(summary = "Exporta o relatório financeiro do usuário em PDF")
    public ResponseEntity<byte[]> exportarPdf() {
        // Gera o PDF em memória (byte array)
        byte[] pdf = relatorioService.gerarRelatorioPdf();

        // Cria nome do arquivo dinâmico com data de hoje
        // Exemplo: "relatorio-geristreams-2025-01-15.pdf"
        String nomeArquivo = "relatorio-geristreams-" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";

        // Constrói resposta HTTP com headers especiais para download
        return ResponseEntity.ok()
                // CONTENT_DISPOSITION = "attachment": faz download (não abre inline)
                // filename = nome do arquivo para salvar localmente
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                // Content-Type: application/pdf (tipo MIME para PDF)
                .contentType(MediaType.APPLICATION_PDF)
                // body: bytes do PDF
                .body(pdf);
    }
}
