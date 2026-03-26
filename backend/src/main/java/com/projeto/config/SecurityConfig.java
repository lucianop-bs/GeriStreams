package com.projeto.config;

import com.projeto.security.JwtAuthFilter;
import com.projeto.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de Segurança Spring Security.
 * <p>
 * Esta classe define as regras de segurança da aplicação:
 * → Quais endpoints são públicos
 * → Quais requerem autenticação
 * → Quais requerem papéis específicos (ADMIN)
 * → Como validar senhas (BCrypt)
 * → Como funciona a autenticação (JWT)
 *
 * @Configuration: marca como classe de configuração Spring
 * → Métodos @Bean aqui são executados na inicialização
 * → Objetos criados ficam disponíveis para injeção
 * @EnableWebSecurity: ativa segurança web
 * → Sem isto, Spring não aplicaria as regras de segurança
 * → Automaticamente adiciona FilterChain ao pipeline HTTP
 * @EnableMethodSecurity: ativa anotações de segurança em métodos
 * → Permite usar @PreAuthorize, @PostAuthorize, etc em qualquer método
 * → Exemplo: @PreAuthorize("hasRole('ADMIN')") em um método de serviço
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Define a cadeia de filtros de segurança (SecurityFilterChain).
     * <p>
     * O que é FilterChain?
     * → Série de filtros que TODA requisição HTTP passa
     * → Cada filtro pode validar, modificar ou rejeitar a requisição
     * → Ordem importa: filtros são executados em sequência
     * → Parecido com middlewares em Express/Node.js
     * <p>
     * Fluxo de uma requisição:
     * 1. Requisição HTTP chega ao servidor
     * 2. Passa pelos filtros (CORS, CSRF, SessionManagement, Authorization, JWT, etc)
     * 3. Se pass em todos, vai para o Controller
     * 4. Controller retorna resposta
     * 5. Resposta passa pelos filtros novamente
     * 6. Volta para o cliente
     * <p>
     * Configurações abaixo:
     *
     * @Bean @return SecurityFilterChain: objeto que Spring usa para processar requisições
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CORS (Cross-Origin Resource Sharing)
                // Permitir requisições de outros domínios (exemplo: frontend em localhost:4200)
                // .withDefaults(): usa configuração padrão do Spring (segura e funcional)
                // Sem isto, browser bloquearia requisições cross-origin (política same-origin)
                .cors(Customizer.withDefaults())

                // CSRF (Cross-Site Request Forgery)
                // Desabilita proteção CSRF porque usamos JWT (stateless)
                // CSRF é proteção para aplicações session-based
                // JWT é inerentemente seguro contra CSRF (não usa cookies automáticos)
                // Desabilitar é OK em APIs REST modernas com JWT
                .csrf(AbstractHttpConfigurer::disable)

                // Gerenciamento de sessões
                // SessionCreationPolicy.STATELESS: não cria sessões no servidor
                // Cada requisição é independente (autenticação via JWT)
                // Isto permite escalabilidade: qualquer servidor pode processar a requisição
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Autorização: quem pode acessar o quê?
                .authorizeHttpRequests(auth -> auth
                                // ENDPOINTS PÚBLICOS (sem autenticação)
                                // /api/auth/** = login, register
                                // Qualquer pessoa pode acessar (nem precisa estar autenticado)
                        .requestMatchers("/api/auth/**").permitAll()

                                // Swagger/OpenAPI documentation
                                // Frontend e ferramentas precisam acessar sem token
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                                // Actuator - endpoints de monitoramento
                        .requestMatchers("/actuator/**").permitAll()

                                // ENDPOINTS ADMINISTRATIVOS (apenas ADMIN)
                                // /api/admin/** = rotas de administração
                                // Apenas usuários com papel ADMIN podem acessar
                                // Qualquer outro papel (USER) recebe 403 Forbidden
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                .anyRequest()
                                .authenticated()

                        // ENDPOINTS PROTEGIDOS (qualquer autenticado)
                        // anyRequest(): qualquer outra rota não especificada acima
                        // .authenticated(): requer estar autenticado (ter um JWT válido)
                        // Usuários comuns (USER) podem acessar, mas não admins
                )

                // AuthenticationProvider: como validar usuário+senha
                // DaoAuthenticationProvider: busca usuário no banco, compara senhas
                .authenticationProvider(authenticationProvider())

                // Adiciona o filtro JWT na cadeia
                // addFilterBefore(jwtAuthFilter, ...): executa ANTES do filtro de username/password
                // UsernamePasswordAuthenticationFilter: filtro padrão do Spring
                // Isto garante que JWT é validado antes de tentar session-based auth
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    /**
     * Configura como validar credenciais (usuario + senha).
     * <p>
     * AuthenticationProvider: estratégia de autenticação
     * → Define como o Spring valida username+password
     * → Default: busca no banco e compara senhas
     * <p>
     * DaoAuthenticationProvider:
     * → DAO = Data Access Object
     * → Busca usuário no banco via UserDetailsService
     * → Compara senha fornecida com hash armazenado
     * → Sem matchagem, lança BadCredentialsException
     * <p>
     * Fluxo durante login:
     * 1. Cliente envia email + senha
     * 2. AuthenticationManager chama o AuthenticationProvider
     * 3. Provider chama userDetailsService.loadUserByUsername(email)
     * 4. Provider usa passwordEncoder.matches(senha, hash) para validar
     * 5. Se tudo OK: cria Authentication com authorities
     * 6. Se falhar: lança BadCredentialsException
     *
     * @return AuthenticationProvider configurado
     * @Bean: Spring criará este objeto e reutilizará
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        // Define como buscar usuários (pelo email no banco)
        provider.setUserDetailsService(userDetailsService);

        // Define como validar senhas (BCrypt com salt)
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    /**
     * Obtém o gerenciador de autenticação do Spring.
     * <p>
     * AuthenticationManager: orquestrador central de autenticação
     * → Delega para AuthenticationProviders
     * → Gerencia a validação de credenciais
     * → Retorna Authentication se sucesso
     * <p>
     * Quando usado:
     * → Em AuthService.login(): para validar email + senha
     * → authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(...))
     *
     * @param config Configuração do Spring (fornecida automaticamente)
     * @return AuthenticationManager
     * @throws Exception se não conseguir criar
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configuração do codificador de senhas: BCrypt.
     * <p>
     * O que é BCrypt?
     * → Algoritmo de hashing de senhas (unidirecional)
     * → NÃO é reversível: não dá pra recuperar a senha original
     * → Inclui "salt" (valor aleatório) automaticamente
     * → Torna ataques de força bruta muito caros (lentos)
     * <p>
     * Por que BCrypt em vez de SHA-256 ou MD5?
     * → SHA e MD5 são rápidos demais (bom para hashes, ruim para senhas)
     * → Rápido = ataque de força bruta rápido também
     * → BCrypt é intencionalmente lento (ajustável)
     * → Cada hash leva ~1 segundo (seguro)
     * <p>
     * Como funciona:
     * → passwordEncoder.encode("123456") retorna hash como "$2a$10$..."
     * → Cada execução produz hash diferente (por causa do salt)
     * → passwordEncoder.matches("123456", hash) valida sem revelar a senha
     * <p>
     * Segurança:
     * → Nunca armazene senhas em texto puro!
     * → Sempre use BCrypt (ou Argon2, PBKDF2)
     * → Incluir salt torna impossível usar tabelas de rainbow
     *
     * @return BCryptPasswordEncoder configurado
     * @Bean: Spring criará um PasswordEncoder (BCrypt)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
