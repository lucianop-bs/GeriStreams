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

/**
 * Controller de Assinaturas.
 * <p>
 * Gerencia o CRUD e operações de assinaturas digitais.
 * <p>
 * Endpoints:
 * → GET /api/subscriptions: listar assinaturas
 * → POST /api/subscriptions: criar nova assinatura
 * → PUT /api/subscriptions/{id}: atualizar assinatura
 * → DELETE /api/subscriptions/{id}: remover assinatura
 * → PATCH /api/subscriptions/{id}/toggle: ativar/desativar
 * → GET /api/subscriptions/resumo: resumo financeiro
 * <p>
 * TODAS as rotas REQUEREM autenticação (token JWT válido).
 * <p>
 * Segurança por isolamento:
 * → Usuário comum vê APENAS suas próprias assinaturas
 * → Não consegue acessar assinaturas de outro usuário
 * → AssinaturaService filtra automaticamente por usuarioId
 */
@RestController
@RequestMapping("/api/subscriptions")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Assinaturas")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    public AssinaturaController(AssinaturaService assinaturaService) {
        this.assinaturaService = assinaturaService;
    }

    /**
     * Lista todas as assinaturas do usuário autenticado.
     * <p>
     * Verbo HTTP: GET (obter dados)
     * Rota: GET /api/subscriptions
     * Autenticação: REQUERIDA
     * <p>
     * Retorna: Lista de todas as assinaturas (ativas e inativas) do usuário
     * Status: 200 OK
     * <p>
     * Fluxo:
     * 1. Frontend faz GET com JWT
     * 2. JwtAuthFilter valida token
     * 3. Controller chama service.listar()
     * 4. Service obtém usuário autenticado
     * 5. Service busca assinaturas daquele usuário (filtro automático)
     * 6. Service converte para DTOs
     * 7. Controller retorna 200 + lista
     *
     * @return ResponseEntity com status 200 OK e lista de DTOs
     */
    @GetMapping
    @Operation(summary = "Lista todas as assinaturas do usuário autenticado")
    public ResponseEntity<List<AssinaturaResponseDTO>> listar() {
        return ResponseEntity.ok(assinaturaService.listar());
    }

    /**
     * Cria uma nova assinatura para o usuário autenticado.
     * <p>
     * Verbo HTTP: POST (criar novo recurso)
     * Rota: POST /api/subscriptions
     * Autenticação: REQUERIDA
     * Status de resposta: 201 CREATED (padrão para criação)
     * <p>
     * Corpo da requisição (JSON):
     * {
     * "nome": "Netflix",
     * "valor": 50.90,
     * "categoria": "STREAMING_VIDEO"
     * }
     * <p>
     * O que @Valid faz?
     * → Valida o DTO antes de processar
     * → Verifica: nome não vazio? valor > 0? categoria é válida?
     * → Se falhar: 400 Bad Request automático
     *
     * @param dto Dados da assinatura (nome, valor, categoria)
     * @return ResponseEntity com status 201 e DTO da assinatura criada (com ID)
     * @PathVariable vs @RequestBody:
     * → @RequestBody: dados vêm do corpo (JSON)
     * → @PathVariable: dados vêm da URL (/api/subscriptions/{id})
     * <p>
     * Fluxo:
     * 1. Frontend envia POST com JSON
     * 2. Spring deserializa em AssinaturaRequestDTO
     * 3. @Valid valida
     * 4. Controller chama service.criar(dto)
     * 5. Service obtém usuário autenticado
     * 6. Service cria entidade Assinatura
     * 7. Service salva no banco (JPA gera ID automaticamente)
     * 8. Service retorna DTO com ID preenchido
     * 9. Controller retorna 201 CREATED + DTO
     */
    @PostMapping
    @Operation(summary = "Adiciona uma nova assinatura")
    public ResponseEntity<AssinaturaResponseDTO> criar(@Valid @RequestBody AssinaturaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assinaturaService.criar(dto));
    }

    /**
     * Atualiza uma assinatura existente.
     * <p>
     * Verbo HTTP: PUT (atualizar recurso)
     * Rota: PUT /api/subscriptions/{id}
     * Autenticação: REQUERIDA
     * Status: 200 OK
     * <p>
     * Parâmetro de URL:
     * → @PathVariable Long id: o {id} vira um parâmetro Java
     * → Exemplo: PUT /api/subscriptions/123 → id = 123L
     * <p>
     * Corpo (JSON):
     * {
     * "nome": "Netflix Premium",
     * "valor": 55.90,
     * "categoria": "STREAMING_VIDEO"
     * }
     * <p>
     * Segurança:
     * → AssinaturaService.buscarPorIdDoUsuario() valida que é do usuário
     * → Se ID não pertence ao usuário: 400 Bad Request
     * → Impossível um usuário atualizar assinatura de outro
     * <p>
     * Fluxo:
     * 1. Frontend envia PUT /api/subscriptions/5 com JSON
     * 2. Spring extrai ID = 5 da URL
     * 3. Spring deserializa JSON em DTO
     * 4. @Valid valida
     * 5. Controller chama service.atualizar(5, dto)
     * 6. Service busca assinatura verificando propriedade
     * 7. Service atualiza campos
     * 8. Service salva (JPA faz UPDATE porque tem ID)
     * 9. Service retorna DTO atualizado
     * 10. Controller retorna 200 OK + DTO
     *
     * @param id  ID da assinatura a atualizar
     * @param dto Novos dados
     * @return ResponseEntity com status 200 e DTO atualizado
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma assinatura existente")
    public ResponseEntity<AssinaturaResponseDTO> atualizar(@PathVariable Long id,
                                                           @Valid @RequestBody AssinaturaRequestDTO dto) {
        return ResponseEntity.ok(assinaturaService.atualizar(id, dto));
    }

    /**
     * Deleta uma assinatura.
     * <p>
     * Verbo HTTP: DELETE (remover recurso)
     * Rota: DELETE /api/subscriptions/{id}
     * Autenticação: REQUERIDA
     * Status: 204 NO CONTENT (sucesso, sem corpo na resposta)
     * <p>
     * Por que 204 em vez de 200?
     * → 204 = "Sucesso, nada para retornar"
     * → Convenção HTTP para DELETE bem-sucedido
     * → Não há DTO de resposta (assinatura foi deletada)
     * <p>
     * ResponseEntity<Void>:
     * → "Void" = sem dados na resposta
     * → Apenas status HTTP (204 NO CONTENT)
     * <p>
     * Fluxo:
     * 1. Frontend envia DELETE /api/subscriptions/5
     * 2. Controller chama service.remover(5)
     * 3. Service busca e valida propriedade
     * 4. Service deleta do banco
     * 5. Service retorna (sem DTO, foi deletado)
     * 6. Controller retorna 204 NO CONTENT (sem corpo)
     *
     * @param id ID da assinatura a deletar
     * @return ResponseEntity com status 204 (sem corpo)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma assinatura")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        assinaturaService.remover(id);
        // ResponseEntity.noContent(): cria resposta 204 NO CONTENT
        return ResponseEntity.noContent().build();
    }

    /**
     * Alterna o status (ativo/inativo) de uma assinatura.
     * <p>
     * Verbo HTTP: PATCH (atualização parcial)
     * Rota: PATCH /api/subscriptions/{id}/toggle
     * Autenticação: REQUERIDA
     * Status: 200 OK
     * <p>
     * O que é PATCH?
     * → PUT: atualiza recurso completo
     * → PATCH: atualiza apenas alguns campos
     * → Aqui: alterna apenas o campo "ativo"
     * <p>
     * O que significa "toggle"?
     * → Alterna entre dois estados (ativo ↔ inativo)
     * → Se estava ativo: vira inativo
     * → Se estava inativo: vira ativo
     * <p>
     * Utilidade:
     * → Pausar uma assinatura sem deletá-la
     * → Assinaturas inativas não contam no resumo financeiro
     * → Pode ser reativada depois
     * <p>
     * Fluxo:
     * 1. Frontend envia PATCH /api/subscriptions/5/toggle
     * 2. Controller chama service.alternarAtivo(5)
     * 3. Service busca assinatura
     * 4. Service inverte o booleano (! ativo)
     * 5. Service salva
     * 6. Service retorna DTO com novo status
     * 7. Controller retorna 200 OK + DTO
     *
     * @param id ID da assinatura a alternar
     * @return ResponseEntity com status 200 e DTO com status atualizado
     */
    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Ativa ou desativa uma assinatura")
    public ResponseEntity<AssinaturaResponseDTO> alternarAtivo(@PathVariable Long id) {
        return ResponseEntity.ok(assinaturaService.alternarAtivo(id));
    }

    /**
     * Calcula resumo financeiro do usuário.
     * <p>
     * Verbo HTTP: GET (obter dados)
     * Rota: GET /api/subscriptions/resumo
     * Autenticação: REQUERIDA
     * Status: 200 OK
     * <p>
     * Retorna:
     * {
     * "salario": 5000.00,
     * "totalAssinaturas": 385.50,
     * "percentualGasto": 7.71,
     * "assinaturas": [...],
     * "gastosPorCategoria": {
     * "STREAMING_VIDEO": 250.00,
     * "STREAMING_MUSICA": 80.00,
     * "SOFTWARE": 55.50
     * }
     * }
     * <p>
     * Cálculos:
     * → salario: do usuário (BigDecimal)
     * → totalAssinaturas: SUM de assinaturas ATIVAS
     * → percentualGasto: (total / salario) × 100
     * → gastosPorCategoria: agrupamento por categoria
     * <p>
     * Útil para:
     * → Dashboard do frontend
     * → Visualizar quanto está gastando
     * → Gráfico de distribuição por categoria
     * → Alertar se gasto > 50% do salário
     * <p>
     * Fluxo:
     * 1. Frontend faz GET /api/subscriptions/resumo
     * 2. Controller chama service.calcularResumoFinanceiro()
     * 3. Service executa múltiplas queries:
     * - SUM de valores ativos
     * - GROUP BY categoria
     * 4. Service faz cálculos (percentual = total / salario * 100)
     * 5. Service agrupa em DTO
     * 6. Controller retorna 200 OK + DTO
     *
     * @return ResponseEntity com status 200 e ResumoFinanceiroDTO
     */
    @GetMapping("/resumo")
    @Operation(summary = "Retorna o resumo financeiro com cálculo de gastos em relação ao salário")
    public ResponseEntity<ResumoFinanceiroDTO> resumoFinanceiro() {
        return ResponseEntity.ok(assinaturaService.calcularResumoFinanceiro());
    }
}
