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

/**
 * Controller Administrativo.
 * <p>
 * ENDPOINTS RESTRITOS: apenas usuários com papel ADMIN podem acessar.
 * <p>
 * Funcionalidades:
 * → Ver todos os usuários cadastrados
 * → Ver assinaturas de qualquer usuário
 * → Promover usuários para ADMIN
 * → Acessar rankings globais
 * <p>
 * Segurança:
 * → @PreAuthorize("hasRole('ADMIN')") na classe: TODA requisição passa por esta validação
 * → Se USER tenta acessar: recebe 403 Forbidden
 * → Se token inválido/expirado: recebe 401 Unauthorized
 * → Se usuário não autenticado: recebe 401
 *
 * @PreAuthorize("hasRole('ADMIN')"): → Anotação do Spring Security
 * → Executa ANTES do método
 * → Verifica se usuário tem papel "ADMIN"
 * → Se não: lança AccessDeniedException (capturada por GlobalExceptionHandler)
 * <p>
 * Quando usar endpoints admin?
 * → Monitoramento de usuários
 * → Estatísticas globais
 * → Promover novos admins
 * → Análise de dados da plataforma
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")  // Proteção em nível de classe: todos precisam ser ADMIN
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin")
public class AdminController {

    private final UsuarioService usuarioService;
    private final AssinaturaService assinaturaService;

    public AdminController(UsuarioService usuarioService, AssinaturaService assinaturaService) {
        this.usuarioService = usuarioService;
        this.assinaturaService = assinaturaService;
    }

    /**
     * Lista TODOS os usuários cadastrados no sistema.
     * <p>
     * Rota: GET /api/admin/users
     * Acesso: ADMIN APENAS
     * Status: 200 OK
     * <p>
     * Diferença de GET /api/users/me (endpoint de usuário comum):
     * → /api/users/me: retorna dados do PRÓPRIO usuário
     * → /api/admin/users: retorna TODOS os usuários (visão administrativa)
     * <p>
     * Fluxo:
     * 1. Admin faz GET com token
     * 2. @PreAuthorize valida: é ADMIN?
     * 3. Se não: 403 Forbidden
     * 4. Se sim: chama usuarioService.listarTodos()
     * 5. Retorna lista de todos os usuários
     * <p>
     * Utilidade:
     * → Dashboard administrativo
     * → Monitorar base de usuários
     * → Ver quantos usuários tem cadastrados
     * → Identificar usuários para promover/gerenciar
     *
     * @return ResponseEntity com lista de todos os usuários (DTOs)
     */
    @GetMapping("/users")
    @Operation(summary = "Lista todos os usuários cadastrados")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    /**
     * Busca um usuário específico pelo ID.
     * <p>
     * Rota: GET /api/admin/users/{id}
     * Acesso: ADMIN APENAS
     * Status: 200 OK
     *
     * @param id ID do usuário a buscar
     * @return ResponseEntity com DTO do usuário
     * @PathVariable Long id:
     * → {id} na URL é transformado em parâmetro Java
     * → Exemplo: GET /api/admin/users/123 → id = 123L
     * <p>
     * Fluxo:
     * 1. Admin faz GET /api/admin/users/42
     * 2. Controller extrai id = 42
     * 3. Chama usuarioService.buscarPorId(42)
     * 4. Se não encontra: 400 Bad Request ("Usuário não encontrado")
     * 5. Se encontra: retorna DTO
     * <p>
     * Utilidade:
     * → Ver detalhes de um usuário específico
     * → Email, salário, role, data de criação
     * → Antes de promover a ADMIN (verificar dados)
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "Retorna os dados de um usuário específico")
    public ResponseEntity<UsuarioResponseDTO> buscarUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    /**
     * Lista TODAS as assinaturas de um usuário específico.
     * <p>
     * Rota: GET /api/admin/users/{id}/subscriptions
     * Acesso: ADMIN APENAS
     * Status: 200 OK
     * <p>
     * Diferença de GET /api/subscriptions (endpoint de usuário comum):
     * → /api/subscriptions: retorna assinaturas do PRÓPRIO usuário
     * → /api/admin/users/{id}/subscriptions: retorna assinaturas de OUTRO usuário
     * <p>
     * Por que um admin precisa disto?
     * → Suporte: usuário relata problema com assinatura
     * → Admin vê as assinaturas do usuário para ajudar
     * → Auditoria: revisar gastos de um usuário específico
     * <p>
     * Fluxo:
     * 1. Admin faz GET /api/admin/users/42/subscriptions
     * 2. Controller extrai id = 42
     * 3. Chama assinaturaService.listarPorUsuario(42)
     * 4. Retorna assinaturas daquele usuário
     *
     * @param id ID do usuário cujas assinaturas serão listadas
     * @return ResponseEntity com lista de assinaturas (DTOs)
     */
    @GetMapping("/users/{id}/subscriptions")
    @Operation(summary = "Lista todas as assinaturas de um usuário específico")
    public ResponseEntity<List<AssinaturaResponseDTO>> listarAssinaturasDoUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(assinaturaService.listarPorUsuario(id));
    }

    /**
     * Promove um usuário comum (USER) para administrador (ADMIN).
     * <p>
     * Rota: PATCH /api/admin/users/{id}/promote
     * Acesso: ADMIN APENAS (lógica: um ADMIN promove outro usuário)
     * Status: 200 OK
     * <p>
     * Por que PATCH?
     * → PATCH: atualização parcial (muda apenas o role)
     * → PUT: atualizar tudo (não o caso aqui)
     * <p>
     * Caso de uso:
     * → Admin quer confiar em outro usuário
     * → Promove para ADMIN para que possa administrar
     * → Novo ADMIN pode promover mais usuários
     * <p>
     * Regras de negócio (implementadas em UsuarioService):
     * 1. Admin não pode se auto-promover (já é ADMIN)
     * 2. Usuário que já é ADMIN não pode ser promovido novamente
     * 3. Só ADMIN pode fazer promoção (protegido por @PreAuthorize)
     * <p>
     * Fluxo:
     * 1. Admin1 faz PATCH /api/admin/users/42/promote
     * 2. Service obtém Admin1 do contexto (para validar)
     * 3. Service busca usuário 42
     * 4. Service valida regras (não é auto-promoção? não é dupla promoção?)
     * 5. Service muda role para ADMIN
     * 6. Service salva no banco
     * 7. Retorna DTO atualizado
     * 8. Admin1 agora pode contar com Admin2
     * <p>
     * Segurança:
     * → Apenas 1 ADMIN não pode se promover sozinho (lógica previne)
     * → Primeira promoção: desenvolvedor insere no banco manualmente
     * → Futuras: ADMIN1 promove ADMIN2, ADMIN2 promove ADMIN3, etc
     *
     * @param id ID do usuário a promover
     * @return ResponseEntity com DTO do usuário promovido (role = ADMIN agora)
     */
    @PatchMapping("/users/{id}/promote")
    @Operation(summary = "Promove um usuário para administrador")
    public ResponseEntity<UsuarioResponseDTO> promoverParaAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.promoverParaAdmin(id));
    }

    /**
     * Ranking de SERVIÇOS mais populares (mais assinados e mais gasto).
     * <p>
     * Rota: GET /api/admin/ranking
     * Acesso: ADMIN APENAS
     * Status: 200 OK
     * <p>
     * Retorna para cada serviço:
     * → Nome (ex: "Netflix")
     * → Quantidade de usuários que assinam
     * → Gasto total (por todos os usuários)
     * → Média de gasto por assinatura
     * <p>
     * Exemplo de resultado:
     * [
     * {
     * "nome": "Netflix",
     * "quantidadeUsuarios": 250,
     * "gastoTotal": 12500.00,
     * "gastoMedio": 50.00
     * },
     * {
     * "nome": "Spotify",
     * "quantidadeUsuarios": 300,
     * "gastoTotal": 8400.00,
     * "gastoMedio": 28.00
     * }
     * ]
     * <p>
     * Utilidade para admin:
     * → Identificar serviços mais populares
     * → Análise de tendências
     * → Informação para parcerias/negócios
     * → Relatórios executivos
     * → Saber que Netflix é lider, depois Spotify, etc
     * <p>
     * Query complexa (AssinaturaRepository.rankingServicos()):
     * → GROUP BY nome: agrupa por nome de serviço
     * → COUNT(*): quantas assinaturas daquele nome
     * → SUM(valor): quanto foi gasto no total
     * → AVG(valor): qual a média
     * → ORDER BY SUM(valor) DESC: maior gasto primeiro
     *
     * @return ResponseEntity com lista de RankingAssinaturaDTO
     */
    @GetMapping("/ranking")
    @Operation(summary = "Ranking dos serviços de assinatura mais usados e com maior gasto total")
    public ResponseEntity<List<RankingAssinaturaDTO>> rankingServicos() {
        return ResponseEntity.ok(assinaturaService.rankingServicos());
    }
}
