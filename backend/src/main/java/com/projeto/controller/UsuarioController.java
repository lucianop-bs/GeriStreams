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

/**
 * Controller de Usuário.
 * <p>
 * Endpoints relacionados ao usuário autenticado:
 * → GET /api/users/me: retorna dados do perfil
 * → PUT /api/users/me/salario: atualiza o salário
 * <p>
 * TODAS as rotas aqui REQUEREM autenticação (token JWT válido).
 *
 * @SecurityRequirement(name = "bearerAuth"):
 * → Anotação OpenAPI/Swagger
 * → Indica que todos os endpoints usam Bearer Token (JWT)
 * → No Swagger UI, mostra um botão "Authorize" para informar token
 * → Documento fica claro: estas rotas são protegidas
 * @Tag(name = "Usuário"):
 * → Agrupa endpoints nesta classe sob a categoria "Usuário" no Swagger
 */
@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuário")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Retorna os dados do perfil do usuário autenticado.
     * <p>
     * Verbo HTTP: GET (obter dados, sem efeitos colaterais)
     * Rota: GET /api/users/me
     * Autenticação: REQUERIDA (JWT no header Authorization)
     * Acesso: Qualquer usuário autenticado (USER ou ADMIN)
     * <p>
     * O que é "/me"?
     * → Convenção REST para "eu mesmo / meu próprio recurso"
     * → Em vez de GET /api/users/{id}, usa /me (mais seguro e óbvio)
     * → Retorna dados do usuário que fez a requisição (obtido do JWT)
     * <p>
     * Fluxo:
     * 1. Frontend envia GET com token JWT no header Authorization
     * 2. JwtAuthFilter valida o token, extrai email do usuário
     * 3. SecurityContextHolder armazena o usuário autenticado
     * 4. Controller chama usuarioService.buscarPerfil()
     * 5. Service obtém usuário do contexto de segurança (SecurityContextHolder)
     * 6. Service retorna DTO (sem dados sensíveis como senha)
     * 7. Controller retorna 200 OK + DTO
     * <p>
     * O que é UsuarioResponseDTO?
     * → DTO que contém: id, nome, email, salario, role, createdAt
     * → NÃO contém: senha (nunca envie senha ao frontend!)
     * → Simples, seguro, estruturado
     *
     * @return ResponseEntity com status 200 OK e DTO do usuário
     */
    @GetMapping("/me")
    @Operation(summary = "Retorna o perfil do usuário autenticado")
    public ResponseEntity<UsuarioResponseDTO> buscarPerfil() {
        return ResponseEntity.ok(usuarioService.buscarPerfil());
    }

    /**
     * Atualiza o salário do usuário autenticado.
     * <p>
     * Verbo HTTP: PUT (atualizar recurso completo ou parcial)
     * Rota: PUT /api/users/me/salario
     * Autenticação: REQUERIDA
     * Acesso: Qualquer usuário autenticado
     * <p>
     * Por que PUT e não POST?
     * → POST: criar novo recurso (não o caso aqui)
     * → PUT: atualizar recurso existente (salário já existe, apenas muda)
     * → PATCH: atualização parcial (também seria válido)
     * <p>
     * Fluxo:
     * 1. Frontend envia PUT com JSON: {"salario": 5500.00}
     * 2. @Valid valida o DTO (salario > 0?)
     * 3. JwtAuthFilter valida JWT
     * 4. Controller chama usuarioService.atualizarSalario(dto)
     * 5. Service obtém usuário autenticado
     * 6. Service atualiza o campo salário
     * 7. Service salva no banco
     * 8. Servi retorna DTO atualizado
     * 9. Controller retorna 200 OK + DTO
     * <p>
     * Por que @Valid?
     * → Valida o DTO de entrada antes de processar
     * → Garante que salário é um BigDecimal válido
     * → Se falhar: Spring retorna 400 Bad Request automaticamente
     *
     * @param dto Objeto com novo salário
     * @return ResponseEntity com status 200 OK e DTO atualizado
     */
    @PutMapping("/me/salario")
    @Operation(summary = "Atualiza o salário do usuário autenticado")
    public ResponseEntity<UsuarioResponseDTO> atualizarSalario(@Valid @RequestBody AtualizarSalarioDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarSalario(dto));
    }
}
