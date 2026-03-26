package com.projeto.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Manipulador Global de Exceções (Exception Handler).
 * <p>
 * O que faz?
 * → Centraliza o tratamento de exceções em toda a aplicação
 * → Quando qualquer exceção é lançada em um Controller, ela é capturada aqui
 * → Transforma a exceção em uma resposta HTTP estruturada (JSON)
 * → Evita que erros brutos vazem para o cliente (segurança + UX)
 *
 * @RestControllerAdvice: → "Advice" = componente que oferece conselhos para Controllers
 * → @RestController + @ControllerAdvice
 * → Sem ela: exceção não tratada causa erro 500 com stacktrace
 * → Com ela: resposta JSON estruturada com mensagem amigável
 * @ExceptionHandler(TipoDeExceção.class): → Marca um método para tratar um tipo específico de exceção
 * → Se aquela exceção for lançada em qualquer Controller:
 * 1. Spring a captura
 * 2. Procura por @ExceptionHandler para esse tipo
 * 3. Chama o método do handler
 * 4. Retorna ResponseEntity (resposta HTTP)
 * <p>
 * Fluxo de tratamento:
 * 1. Code (Controller/Service) lança exceção
 * 2. Spring "sobe" procurando por @ExceptionHandler
 * 3. Se encontrar aqui: executa o método
 * 4. Retorna ResponseEntity com JSON
 * 5. Frontend recebe resposta HTTP estruturada
 * <p>
 * Exemplo: quando usuário tenta login com senha errada
 * 1. AuthService.login() lança BadCredentialsException
 * 2. Spring procura por @ExceptionHandler(BadCredentialsException.class)
 * 3. Encontra handleBadCredentials()
 * 4. Retorna 401 + JSON: {"error": "E-mail ou senha inválidos.", "code": 401}
 * 5. Frontend recebe 401 Unauthorized + mensagem
 * <p>
 * Hierarquia de tratamento (mais específico vence):
 * → BadCredentialsException → @ExceptionHandler(BadCredentialsException.class)
 * → Qualquer outra exceção → @ExceptionHandler(Exception.class)
 * → Se não tem handler: erro 500 (default do servidor)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Trata erros de validação de entrada (@Valid).
     * <p>
     * Quando é lançado?
     * → Quando @Valid em @RequestBody falha
     * → Exemplo: nome vazio, email inválido, valor negativo
     * → Spring lança MethodArgumentNotValidException automaticamente
     * <p>
     * MethodArgumentNotValidException:
     * → Exception do Spring que encapsula erros de validação
     * → Contém: lista de campos inválidos + mensagens
     * → ex.getBindingResult(): acesso aos erros
     * <p>
     * Fluxo:
     * 1. Frontend envia POST com JSON: {"salario": -100}
     * 2. Spring valida com @Valid
     * 3. Validação falha (salario < 0 viola @Positive)
     * 4. Spring lança MethodArgumentNotValidException
     * 5. Este handler captura
     * 6. Extrai erros por campo
     * 7. Retorna 400 + JSON com detalhes de cada erro
     * <p>
     * Resposta HTTP:
     * {
     * "error": "Erro de validação",
     * "code": 400,
     * "fields": {
     * "salario": "deve ser maior que 0"
     * }
     * }
     *
     * @param ex Exceção contendo erros de validação
     * @return ResponseEntity com status 400 e detalhes dos erros
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        logger.warn("Erro de validação nos campos da requisição");

        // Cria mapa para armazenar erros por campo
        Map<String, String> fieldErrors = new HashMap<>();

        // ex.getBindingResult().getAllErrors(): obtém lista de erros
        // forEach: itera sobre cada erro
        ex.getBindingResult().getAllErrors().forEach(error -> {
            // error é um ObjectError, precisamos converter para FieldError
            // FieldError: erro associado a um campo específico
            String field = ((FieldError) error).getField();  // Nome do campo (ex: "salario")
            String message = error.getDefaultMessage();      // Mensagem (ex: "deve ser maior que 0")

            // Armazena no mapa: {"salario": "deve ser maior que 0"}
            fieldErrors.put(field, message);
        });

        // Retorna resposta estruturada
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Erro de validação",
                "code", 400,
                "fields", fieldErrors  // Detalhe dos campos com erro
        ));
    }

    /**
     * Trata IllegalArgumentException (erros de lógica de negócio).
     * <p>
     * Quando é lançado?
     * → Services lançam isto para erros esperados
     * → Exemplo: "E-mail já cadastrado", "Usuário não encontrado"
     * → Não é um erro de código, é um problema de dados
     * <p>
     * Exemplos de uso no código:
     * → AuthService: "E-mail já cadastrado"
     * → UsuarioService: "Usuário não encontrado"
     * → AssinaturaService: "Assinatura não encontrada"
     * <p>
     * Fluxo:
     * 1. Service valida regra de negócio
     * 2. Service lança IllegalArgumentException("E-mail já cadastrado")
     * 3. Handler captura
     * 4. Retorna 400 + mensagem
     * 5. Frontend recebe mensagem amigável para mostrar ao usuário
     * <p>
     * Resposta HTTP:
     * {
     * "error": "E-mail já cadastrado.",
     * "code": 400
     * }
     * <p>
     * Status 400:
     * → "Bad Request" (requisição inválida)
     * → Cliente fez uma requisição que não pode ser processada
     * → Ex: tentar cadastrar email duplicado
     *
     * @param ex Exceção com mensagem de erro de negócio
     * @return ResponseEntity com status 400 e mensagem
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Erro de negócio: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(Map.of(
                "error", ex.getMessage(),  // Mensagem vem do Service
                "code", 400
        ));
    }

    /**
     * Trata BadCredentialsException (senha/email inválidos).
     * <p>
     * Quando é lançado?
     * → AuthenticationManager.authenticate() durante login
     * → Lançado automaticamente se email ou senha estiverem errados
     * → Não precisamos lançar manualmente, Spring faz
     * <p>
     * Fluxo durante login:
     * 1. Frontend envia email + senha
     * 2. AuthService.login() chama authenticationManager.authenticate()
     * 3. AuthenticationManager compara senha com hash no banco
     * 4. Se não coincide: lança BadCredentialsException
     * 5. Handler captura
     * 6. Retorna 401 + mensagem
     * <p>
     * Status 401:
     * → "Unauthorized" (não autenticado)
     * → Credenciais inválidas
     * → Resposta genérica (não diz se email existe, só que email/senha está errado)
     * → Isto é segurança: não quer vazar quais emails existem
     * <p>
     * Resposta HTTP:
     * {
     * "error": "E-mail ou senha inválidos.",
     * "code": 401
     * }
     * <p>
     * Mensagem genérica por segurança:
     * → Se disse "email não existe", hacker sabe quais emails não têm conta
     * → Se disse "senha errada", hacker sabe email existe mas não sabe senha
     * → Mensagem única: "email ou senha", hacker não fica com informação
     *
     * @param ex Exceção de credenciais inválidas
     * @return ResponseEntity com status 401 e mensagem genérica
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        logger.warn("Tentativa de login com credenciais inválidas");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "E-mail ou senha inválidos.",
                "code", 401
        ));
    }

    /**
     * Trata AccessDeniedException (falta de permissão).
     * <p>
     * Quando é lançado?
     * → Usuário não tem role/autoridade necessária
     * → Exemplo: USER tenta acessar /api/admin/** (precisa ADMIN)
     * → Lançado pelo Spring Security (@PreAuthorize, @Secured, etc)
     * <p>
     * Fluxo:
     * 1. User (papel USER) faz GET /api/admin/usuarios
     * 2. Controller tem @PreAuthorize("hasRole('ADMIN')")
     * 3. Spring valida: USER != ADMIN?
     * 4. Spring lança AccessDeniedException
     * 5. Handler captura
     * 6. Retorna 403 + mensagem
     * <p>
     * Status 403:
     * → "Forbidden" (proibido)
     * → Diferente de 401: usuário ESTÁ autenticado, mas não tem permissão
     * → 401: sem autenticação (sem token válido)
     * → 403: com autenticação, mas autorização negada
     * <p>
     * Resposta HTTP:
     * {
     * "error": "Acesso negado.",
     * "code": 403
     * }
     * <p>
     * Exemplo de cenário:
     * → Usuário faz login (201 = sucesso, token gerado)
     * → Usuário tenta GET /api/admin/usuarios com token
     * → Token é válido, mas usuário é USER, não ADMIN
     * → Retorna 403 (autenticado, mas sem autorização)
     *
     * @param ex Exceção de acesso negado
     * @return ResponseEntity com status 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        logger.warn("Acesso negado: usuário sem permissão para o recurso solicitado");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "Acesso negado.",
                "code", 403
        ));
    }

    /**
     * Trata QUALQUER outra exceção não capturada pelos handlers acima.
     * <p>
     * Quando é lançado?
     * → NullPointerException (bug no código)
     * → IndexOutOfBoundsException (array fora de índice)
     * → ClassCastException (cast inválido)
     * → Qualquer Exception que não tem handler específico
     * <p>
     * Por que um handler genérico?
     * → Sem ele: exception não tratada vaza para o cliente
     * → Cliente recebe stacktrace (horrível para UX)
     * → Stacktrace pode expor detalhes sensíveis (caminhos, SQL, etc)
     * → Com handler: mensagem genérica, segura, amigável
     * <p>
     * Fluxo:
     * 1. Código lança NullPointerException
     * 2. Spring procura por handler específico (não tem)
     * 3. Procura por handler genérico: Exception (tem!)
     * 4. Chama handleGeneric()
     * 5. Retorna 500 + "Erro interno no servidor"
     * <p>
     * Status 500:
     * → "Internal Server Error" (erro no servidor)
     * → É responsabilidade do servidor, não da requisição
     * → Indica bug no código (não esperado)
     * → Em produção: deve-se logar o erro completo (stacktrace)
     * <p>
     * Resposta HTTP:
     * {
     * "error": "Erro interno no servidor.",
     * "code": 500
     * }
     * <p>
     * IMPORTANTE: Em produção
     * → Handler genérico deve logar o erro completo
     * → logger.error("Erro não esperado", ex)
     * → Não retornar stacktrace ao cliente (segurança)
     * → Alertar time de desenvolvimento via Slack/email
     *
     * @param ex Qualquer exceção não tratada
     * @return ResponseEntity com status 500 e mensagem genérica
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        logger.error("Erro não esperado durante requisição", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Erro interno no servidor.",
                "code", 500
        ));
    }
}
