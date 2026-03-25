package com.projeto.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.projeto.dto.assinatura.AssinaturaResponseDTO;
import com.projeto.dto.financeiro.ResumoFinanceiroDTO;
import com.projeto.model.Usuario;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class RelatorioService {

    private static final Color COR_PRIMARIA  = new Color(13, 110, 253);
    private static final Color COR_CINZA     = new Color(108, 117, 125);
    private static final Color COR_CABECALHO = new Color(248, 249, 250);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AssinaturaService assinaturaService;
    private final UsuarioService usuarioService;

    public RelatorioService(AssinaturaService assinaturaService, UsuarioService usuarioService) {
        this.assinaturaService = assinaturaService;
        this.usuarioService = usuarioService;
    }

    public byte[] gerarRelatorioPdf() {
        Usuario usuario = usuarioService.getUsuarioAutenticado();
        ResumoFinanceiroDTO resumo = assinaturaService.calcularResumoFinanceiro();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        adicionarCabecalho(doc, usuario);
        adicionarResumo(doc, resumo);
        adicionarTabelaAssinaturas(doc, resumo);
        adicionarGastosPorCategoria(doc, resumo);
        adicionarRodape(doc);

        doc.close();
        return out.toByteArray();
    }

    private void adicionarCabecalho(Document doc, Usuario usuario) throws DocumentException {
        Font fonteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COR_PRIMARIA);
        Font fonteSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 11, COR_CINZA);

        Paragraph titulo = new Paragraph("GeriStreams", fonteTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph subtitulo = new Paragraph("Relatório Financeiro de Assinaturas", fonteSubtitulo);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        subtitulo.setSpacingAfter(4);
        doc.add(subtitulo);

        Font fonteMeta = FontFactory.getFont(FontFactory.HELVETICA, 9, COR_CINZA);
        Paragraph meta = new Paragraph(
                "Gerado em: " + LocalDate.now().format(FORMATTER) + "   |   Usuário: " + usuario.getNome() + "   |   " + usuario.getEmail(),
                fonteMeta
        );
        meta.setAlignment(Element.ALIGN_CENTER);
        meta.setSpacingAfter(16);
        doc.add(meta);

        doc.add(new LineSeparator());
    }

    private void adicionarResumo(Document doc, ResumoFinanceiroDTO resumo) throws DocumentException {
        Font fonteSec = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COR_PRIMARIA);
        Paragraph secao = new Paragraph("Resumo Financeiro", fonteSec);
        secao.setSpacingBefore(16);
        secao.setSpacingAfter(8);
        doc.add(secao);

        PdfPTable tabela = new PdfPTable(2);
        tabela.setWidthPercentage(60);
        tabela.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabela.setSpacingAfter(12);

        adicionarLinhaResumo(tabela, "Salário mensal",         "R$ " + formatar(resumo.salario()));
        adicionarLinhaResumo(tabela, "Gasto total ativo",      "R$ " + formatar(resumo.totalMensal()));
        adicionarLinhaResumo(tabela, "% do salário comprometido",
                resumo.percentualDoSalario().setScale(1, RoundingMode.HALF_UP) + "%");
        adicionarLinhaResumo(tabela, "Assinaturas ativas",     String.valueOf(resumo.assinaturas().size()));

        doc.add(tabela);
    }

    private void adicionarTabelaAssinaturas(Document doc, ResumoFinanceiroDTO resumo) throws DocumentException {
        if (resumo.assinaturas().isEmpty()) return;

        Font fonteSec = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COR_PRIMARIA);
        Paragraph secao = new Paragraph("Assinaturas Ativas", fonteSec);
        secao.setSpacingBefore(8);
        secao.setSpacingAfter(8);
        doc.add(secao);

        PdfPTable tabela = new PdfPTable(3);
        tabela.setWidthPercentage(100);
        tabela.setWidths(new float[]{4, 3, 2});
        tabela.setSpacingAfter(12);

        adicionarCabecalhoTabela(tabela, "Serviço", "Categoria", "Valor/mês");

        Font fonteCell = FontFactory.getFont(FontFactory.HELVETICA, 10);
        boolean linha = false;
        for (AssinaturaResponseDTO a : resumo.assinaturas()) {
            Color bg = linha ? Color.WHITE : COR_CABECALHO;
            adicionarCelulaTabela(tabela, a.nome(), fonteCell, bg);
            adicionarCelulaTabela(tabela, a.categoria(), fonteCell, bg);
            adicionarCelulaTabela(tabela, "R$ " + formatar(a.valor()), fonteCell, bg);
            linha = !linha;
        }

        doc.add(tabela);
    }

    private void adicionarGastosPorCategoria(Document doc, ResumoFinanceiroDTO resumo) throws DocumentException {
        if (resumo.gastosPorCategoria().isEmpty()) return;

        Font fonteSec = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COR_PRIMARIA);
        Paragraph secao = new Paragraph("Gastos por Categoria", fonteSec);
        secao.setSpacingBefore(8);
        secao.setSpacingAfter(8);
        doc.add(secao);

        PdfPTable tabela = new PdfPTable(2);
        tabela.setWidthPercentage(60);
        tabela.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabela.setSpacingAfter(12);

        resumo.gastosPorCategoria().forEach((categoria, valor) ->
                adicionarLinhaResumo(tabela, categoria, "R$ " + formatar(valor))
        );

        doc.add(tabela);
    }

    private void adicionarRodape(Document doc) throws DocumentException {
        doc.add(new LineSeparator());
        Font fonte = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, COR_CINZA);
        Paragraph rodape = new Paragraph("Relatório gerado automaticamente pelo GeriStreams · Análise financeira de assinaturas digitais", fonte);
        rodape.setAlignment(Element.ALIGN_CENTER);
        rodape.setSpacingBefore(6);
        doc.add(rodape);
    }

    private void adicionarCabecalhoTabela(PdfPTable tabela, String... colunas) {
        Font fonte = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        for (String col : colunas) {
            PdfPCell cell = new PdfPCell(new Phrase(col, fonte));
            cell.setBackgroundColor(COR_PRIMARIA);
            cell.setPadding(6);
            cell.setBorder(Rectangle.NO_BORDER);
            tabela.addCell(cell);
        }
    }

    private void adicionarCelulaTabela(PdfPTable tabela, String texto, Font fonte, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, fonte));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(222, 226, 230));
        tabela.addCell(cell);
    }

    private void adicionarLinhaResumo(PdfPTable tabela, String label, String valor) {
        Font fonteLabel = FontFactory.getFont(FontFactory.HELVETICA, 10, COR_CINZA);
        Font fonteValor = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        PdfPCell cellLabel = new PdfPCell(new Phrase(label, fonteLabel));
        cellLabel.setBorder(Rectangle.BOTTOM);
        cellLabel.setBorderColor(new Color(222, 226, 230));
        cellLabel.setPadding(5);

        PdfPCell cellValor = new PdfPCell(new Phrase(valor, fonteValor));
        cellValor.setBorder(Rectangle.BOTTOM);
        cellValor.setBorderColor(new Color(222, 226, 230));
        cellValor.setPadding(5);

        tabela.addCell(cellLabel);
        tabela.addCell(cellValor);
    }

    private String formatar(java.math.BigDecimal valor) {
        return valor != null ? valor.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
    }
}
