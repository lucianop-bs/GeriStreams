package com.projeto.service;

import com.projeto.dto.ai.*;
import com.projeto.dto.financeiro.ResumoFinanceiroDTO;
import com.projeto.model.Usuario;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AiService {

    private static final String SYSTEM_PROMPT = """
            Você é um consultor financeiro pessoal especializado em assinaturas de streaming e serviços digitais.
            Seu tom é amigável, direto e encorajador.
            Sempre responda em português brasileiro.
            use **negrito** para pontos importantes e listas numeradas para as dicas.
            Não use emojis em excesso, apenas onde fizer sentido.
            Não pule muitas linhas faça um texto bem divido mas sem tanto espaçamento entre as linhas.
            """;

    private final RestClient anthropicRestClient;
    private final AssinaturaService assinaturaService;
    private final UsuarioService usuarioService;

    @Value("${anthropic.model}")
    private String model;

    public AiService(@Qualifier("anthropicRestClient") RestClient anthropicRestClient,
                     AssinaturaService assinaturaService,
                     UsuarioService usuarioService) {
        this.anthropicRestClient = anthropicRestClient;
        this.assinaturaService = assinaturaService;
        this.usuarioService = usuarioService;
    }

    public AiDicasResponseDTO gerarDicas() {
        ResumoFinanceiroDTO resumo = assinaturaService.calcularResumoFinanceiro();
        Usuario usuario = usuarioService.getUsuarioAutenticado();

        String prompt = construirPrompt(usuario.getNome(), resumo);

        AnthropicRequestDTO requestBody = new AnthropicRequestDTO(
                model,
                1024,
                SYSTEM_PROMPT,
                List.of(new AnthropicMessageDTO("user", prompt)),
                false
        );

        AnthropicResponseDTO response = anthropicRestClient
                .post()
                .uri("/v1/messages")
                .body(requestBody)
                .retrieve()
                .body(AnthropicResponseDTO.class);

        if (response == null) {
            throw new IllegalStateException("Nenhuma resposta recebida da API de IA.");
        }

        return new AiDicasResponseDTO(response.extractText());
    }

    private String construirPrompt(String nome, ResumoFinanceiroDTO resumo) {
        StringBuilder sb = new StringBuilder();

        sb.append("Olá! Me chamo ").append(nome).append(".\n\n");
        sb.append("**Minha situação financeira atual com assinaturas:**\n");
        sb.append("- Salário mensal: R$ ").append(formatar(resumo.salario())).append("\n");
        sb.append("- Gasto total mensal em assinaturas ativas: R$ ").append(formatar(resumo.totalMensal())).append("\n");
        sb.append("- Percentual do salário comprometido: ").append(resumo.percentualDoSalario().setScale(1, RoundingMode.HALF_UP)).append("%\n\n");

        if (!resumo.assinaturas().isEmpty()) {
            sb.append("**Minhas assinaturas ativas:**\n");
            resumo.assinaturas().forEach(a ->
                    sb.append("- ").append(a.nome())
                      .append(" (").append(a.categoria()).append(")")
                      .append(" — R$ ").append(formatar(a.valor())).append("/mês\n")
            );
            sb.append("\n");
        }

        if (!resumo.gastosPorCategoria().isEmpty()) {
            sb.append("**Gastos por categoria:**\n");
            resumo.gastosPorCategoria().forEach((categoria, valor) ->
                    sb.append("- ").append(categoria).append(": R$ ").append(formatar(valor)).append("\n")
            );
            sb.append("\n");
        }

        sb.append("Com base nessas informações, me dê 1 dicas práticas e personalizadas ")
          .append("para eu reduzir meus gastos com assinaturas e melhorar meu orçamento. ")
          .append("Se alguma assinatura parecer redundante ou cara, mencione especificamente. ")
          .append("Seja objetivo e mostre o impacto financeiro de cada sugestão quando possível.");

        return sb.toString();
    }

    private String formatar(BigDecimal valor) {
        return valor != null ? valor.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0,00";
    }
}
