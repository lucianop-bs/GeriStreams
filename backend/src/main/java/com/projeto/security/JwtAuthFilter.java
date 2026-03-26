package com.projeto.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de Autenticação JWT.
 * <p>
 * Este é um dos componentes MAIS IMPORTANTES de segurança!
 * <p>
 * O que faz?
 * → Intercepta TODA requisição HTTP
 * → Busca pelo header Authorization (ex: "Bearer eyJhb...")
 * → Valida o JWT
 * → Se válido: autoriza acesso (coloca usuário no SecurityContextHolder)
 * → Se inválido: bloqueia acesso
 * <p>
 * OncePerRequestFilter:
 * → Classe abstrata do Spring que garante que o filtro roda UMA VEZ por requisição
 * → Protege contra execução múltipla do mesmo filtro
 * → Herança automática de Spring
 * <p>
 * Fluxo de requisição com segurança JWT:
 * 1. Cliente faz requisição HTTP com header: Authorization: Bearer <token>
 * 2. JwtAuthFilter.doFilterInternal() é chamado
 * 3. Extrai o token do header
 * 4. Valida a assinatura do token (é falso? foi alterado?)
 * 5. Extrai email do token
 * 6. Carrega dados do usuário no banco
 * 7. Cria Authentication e coloca no SecurityContextHolder
 * 8. Requisição segue normalmente para o Controller
 * 9. Controller já tem acesso ao usuário autenticado via SecurityContextHolder
 *
 * @Component: Spring instancia automaticamente na inicialização
 * <p>
 * SecurityFilterChain:
 * → Cadeia de filtros que toda requisição passa
 * → Ordem: CORS → CSRF → SessionManagement → JwtAuthFilter → Authorization → Controller
 * → Configurada em SecurityConfig.filterChain()
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Método que executa em cada requisição HTTP.
     * <p>
     * Implementação obrigatória de OncePerRequestFilter.
     * <p>
     * Parâmetros:
     * → HttpServletRequest: dados da requisição (headers, URL, método, etc)
     * → HttpServletResponse: objeto para enviar resposta
     * → FilterChain: referência para passar para o próximo filtro
     * <p>
     * Fluxo:
     * 1. Obtém o header Authorization da requisição
     * 2. Valida se o header existe e tem formato "Bearer <token>"
     * 3. Se não tem, passa para o próximo filtro (deixa passar sem autenticação)
     * 4. Se tem: extrai o token
     * 5. Valida o token (assinatura, expiração)
     * 6. Se válido: cria Authentication e coloca no SecurityContextHolder
     * 7. Passa para o próximo filtro
     * 8. Depois que tudo volta, a resposta passa pelos filtros novamente
     *
     * @param request     Requisição HTTP
     * @param response    Resposta HTTP
     * @param filterChain Cadeia de filtros
     * @throws ServletException se erro relacionado a servlet
     * @throws IOException      se erro de I/O
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // ===== STEP 1: Obter header Authorization =====
        // Exemplo: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...."
        String authHeader = request.getHeader("Authorization");

        // ===== STEP 2: Validar formato do header =====
        // Se header é null ou não começa com "Bearer ": sem JWT
        // Apenas passa para o próximo filtro (requisição continua sem autenticação)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // filterChain.doFilter(): passa para o próximo filtro na cadeia
            // A requisição continua seu caminho (pode chegar ao Controller)
            // Se o Controller/Endpoint exigir autenticação, receberá erro depois
            filterChain.doFilter(request, response);
            return;  // Sai do método
        }

        // ===== STEP 3: Extrair token do header =====
        // "Bearer " tem 7 caracteres, então substring(7) remove isso
        // Exemplo: "Bearer eyJhb..." → "eyJhb..."
        String token = authHeader.substring(7);

        // ===== STEP 4: Extrair email do token =====
        // JwtUtil.extractEmail(): faz parse do token, extrai o subject (que é o email)
        // Se o token for inválido ou expirado, isto pode lançar exceção
        // Por enquanto, vamos tentar extrair (pode ser null se algo der errado)
        String email;
        try {
            email = jwtUtil.extractEmail(token);
        } catch (Exception ex) {
            logger.warn("Token JWT inválido ou expirado: {}", ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ===== STEP 5: Verificar se já há autenticação =====
        // email != null: conseguimos extrair email do token?
        // SecurityContextHolder.getContext().getAuthentication() == null:
        //   → SecurityContextHolder armazena informações do usuário autenticado
        //   → Se getAuthentication() retorna null: usuário não está autenticado ainda
        //   → Se já tem autenticação: alguém já autenticou este usuário (skip)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // ===== STEP 6: Carregar dados do usuário =====
            // userDetailsService.loadUserByUsername(email): busca no banco
            // Retorna: UserDetails com username (email), password, authorities (roles)
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // ===== STEP 7: Validar token contra usuário =====
            // JwtUtil.isTokenValid(token, userDetails):
            //   → Verifica se email do token = email do usuário
            //   → Verifica se token não expirou
            //   → Retorna true APENAS se ambas validações passam
            if (jwtUtil.isTokenValid(token, userDetails)) {

                // ===== STEP 8: Criar Authentication =====
                // UsernamePasswordAuthenticationToken:
                //   → Classe que representa "usuário autenticado"
                //   → Construtor: new UsernamePasswordAuthenticationToken(principal, credentials, authorities)
                //   → principal: quem é (userDetails)
                //   → credentials: senha (null, porque não precisa armazenar)
                //   → authorities: papéis/permissões (ROLE_USER, ROLE_ADMIN)
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                // ===== STEP 9: Adicionar detalhes da requisição =====
                // setDetails(): adiciona contexto da requisição (IP, user-agent, etc)
                // Útil para auditoria/logging
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // ===== STEP 10: Colocar no SecurityContextHolder =====
                // SecurityContextHolder: "cofre" central do Spring Security
                // Armazena o usuário autenticado para este thread
                // Controladores/Serviços podem recuperar via:
                //   SecurityContextHolder.getContext().getAuthentication()
                // Agora qualquer código nesta requisição pode saber quem está autenticado
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("Usuário autenticado via JWT: {}", email);
            }
        }

        // ===== STEP 11: Passar para próximo filtro =====
        // filterChain.doFilter(): continua a cadeia de filtros
        // Se autenticação foi sucesso: usuário está autenticado (SecurityContextHolder tem dados)
        // Se autenticação falhou: SecurityContextHolder vazio (será bloqueado se endpoint exigir auth)
        filterChain.doFilter(request, response);
    }
}
