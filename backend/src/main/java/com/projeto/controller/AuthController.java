package com.projeto.controller;

import com.projeto.dto.auth.JwtResponseDTO;
import com.projeto.dto.auth.LoginRequestDTO;
import com.projeto.dto.auth.RegisterRequestDTO;
import com.projeto.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de Autenticação.
 * <p>
 * Responsável pelos endpoints públicos de autenticação:
 * → POST /api/auth/register: cadastro de novo usuário
 * → POST /api/auth/login: autenticação e obtenção de token JWT
 * <p>
 * Padrão: Controller (camada web/REST)
 * → Recebe requisições HTTP
 * → Valida o DTO de entrada
 * → Chama o Service (lógica de negócio)
 * → Retorna resposta HTTP
 *
 * @RestController: → Combina @Controller + @ResponseBody
 * → Transforma automaticamente o retorno em JSON
 * → Sem ela, seria necessário @ResponseBody em cada método
 * @RequestMapping("/api/auth"): → Define o prefixo para todos os endpoints desta classe
 * → GET /api/auth/register → POST /api/auth/register (completo)
 * @Tag(name = "Autenticação"):
 * → Anotação OpenAPI/Swagger
 * → Agrupa este controller sob a categoria "Autenticação" no Swagger UI
 * → Útil para documentação automática da API
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint para cadastrar um novo usuário no sistema.
     * <p>
     * Verboso HTTP: POST (cria um novo recurso)
     * Rota completa: POST /api/auth/register
     * Acesso: PÚBLICO (não precisa de autenticação)
     * Status de resposta: 201 CREATED (em caso de sucesso)
     * <p>
     * O que @Valid faz?
     * → Valida o DTO de entrada antes de passar ao método
     * → Verifica @NotNull, @Email, @Min, @Max, etc anotadas no RegisterRequestDTO
     * → Se a validação falhar, Spring retorna 400 Bad Request automaticamente
     * → O @Valid é do javax.validation (padrão Java)
     *
     * @param dto Objeto com dados do novo usuário (nome, email, senha, salário)
     * @return ResponseEntity com status 201 e JwtResponseDTO (token, email, role)
     * @RequestBody: → Indica que os dados vêm do corpo da requisição HTTP
     * → Spring automaticamente deserializa o JSON em RegisterRequestDTO
     * → Exemplo: {"nome": "João", "email": "joao@email.com", "senha": "123456", "salario": 5000}
     * <p>
     * ResponseEntity:
     * → Classe que permite controlar a resposta HTTP (corpo, status, headers)
     * → Sem ResponseEntity, seria apenas "return authService.registrar(dto)"
     * → Com ResponseEntity, podemos definir o status 201 CREATED
     * <p>
     * Fluxo:
     * 1. Frontend envia POST com JSON do novo usuário
     * 2. Spring deserializa em RegisterRequestDTO
     * 3. @Valid valida os campos
     * 4. Controller chama authService.registrar()
     * 5. Service cria usuário, gera token JWT
     * 6. Controller retorna 201 + token no JSON
     * 7. Frontend recebe token e salva no localStorage
     */
    @PostMapping("/register")
    @Operation(summary = "Cadastrar novo usuário")
    public ResponseEntity<JwtResponseDTO> registrar(@Valid @RequestBody RegisterRequestDTO dto) {
        // Chama o serviço que contém a lógica de cadastro
        JwtResponseDTO response = authService.registrar(dto);

        // Retorna 201 CREATED + o token no corpo da resposta
        // 201 é o status HTTP padrão para criação de recurso
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint para autenticar um usuário existente.
     * <p>
     * Verbo HTTP: POST (não é GET porque a senha é enviada no corpo)
     * Rota completa: POST /api/auth/login
     * Acesso: PÚBLICO (não precisa de autenticação)
     * Status de resposta: 200 OK (em caso de sucesso)
     * <p>
     * Fluxo:
     * 1. Frontend envia POST com JSON (email, senha)
     * 2. Spring deserializa em LoginRequestDTO
     * 3. @Valid valida (email é válido? senha não é vazia?)
     * 4. Controller chama authService.login()
     * 5. Service verifica credenciais, gera token JWT
     * 6. Controller retorna 200 + token no JSON
     * 7. Frontend recebe token, salva no localStorage
     * 8. Frontend inclui token em requisições futuras (header Authorization: Bearer <token>)
     * <p>
     * ResponseEntity.ok():
     * → Retorna status 200 OK
     * → Equivalente a ResponseEntity.status(HttpStatus.OK)
     * → Mais conciso
     *
     * @param dto Objeto com email e senha do usuário
     * @return ResponseEntity com status 200 e JwtResponseDTO (token, email, role)
     * @throws BadCredentialsException se email ou senha incorretos
     */
    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário e obter token JWT")
    public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        // Chama o serviço que autentica o usuário
        JwtResponseDTO response = authService.login(dto);

        // Retorna 200 OK + token no corpo
        return ResponseEntity.ok(response);
    }
}
