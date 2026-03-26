package com.projeto.service;

import com.projeto.dto.auth.JwtResponseDTO;
import com.projeto.dto.auth.LoginRequestDTO;
import com.projeto.dto.auth.RegisterRequestDTO;
import com.projeto.model.Usuario;
import com.projeto.repository.UsuarioRepository;
import com.projeto.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Serviço de Autenticação (Business Logic).
 * <p>
 * Esta classe concentra toda a lógica de negócio relacionada a autenticação:
 * → Cadastro de novos usuários (registro)
 * → Login e geração de token JWT
 * → Validação de credenciais
 * <p>
 * Padrão: Service (BO - Business Object)
 * → O Controller chama este serviço, nunca acessa diretamente o banco
 * → Toda regra de negócio fica aqui, não no Controller
 * <p>
 * Dependências injetadas:
 * → UsuarioRepository: acesso ao banco de dados
 * → PasswordEncoder: criptografia de senhas (BCrypt)
 * → JwtUtil: geração de tokens JWT
 * → AuthenticationManager: autenticação do Spring Security
 * → UserDetailsService: carregamento de usuários para autenticação
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    // Constructor que recebe todas as dependências (injeção de dependências via construtor)
    // Esta é a forma moderna e preferida do Spring — mais testável e segura que @Autowired
    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Registra um novo usuário no sistema.
     * <p>
     * Fluxo:
     * 1. Verifica se o e-mail já está cadastrado (unicidade)
     * 2. Cria uma nova entidade Usuario
     * 3. Criptografa a senha usando BCrypt
     * 4. Salva o usuário no banco de dados
     * 5. Carrega os dados do usuário e gera um token JWT
     * 6. Retorna o token (para que o frontend possa fazer requisições autenticadas)
     * <p>
     * Por que passwordEncoder.encode()?
     * → Nunca armazenar senhas em texto puro! BCrypt é um algoritmo de hash unidirecional
     * que torna impossível recuperar a senha original a partir do hash.
     * → Exemplo: "123456" vira "$2a$10$..." (cada execução gera um hash diferente)
     *
     * @param dto Objeto com os dados do novo usuário (nome, email, senha, salário)
     * @return JwtResponseDTO contendo o token, email e role do usuário
     * @throws IllegalArgumentException se o email já está cadastrado
     */
    public JwtResponseDTO registrar(RegisterRequestDTO dto) {
        logger.info("Tentativa de registro para o e-mail: {}", dto.email());

        // Regra de negócio: não pode haver dois usuários com o mesmo e-mail
        if (usuarioRepository.existsByEmail(dto.email())) {
            logger.warn("Registro rejeitado: e-mail já cadastrado - {}", dto.email());
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        // Criar nova instância de Usuario
        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());

        // IMPORTANTE: criptografar a senha ANTES de salvar no banco
        usuario.setSenha(passwordEncoder.encode(dto.senha()));

        usuario.setSalario(dto.salario());

        // Salvar no banco. Spring Data JPA cuida de gerar o ID automaticamente
        usuarioRepository.save(usuario);

        // Após salvar, carregar os dados do usuário como UserDetails (padrão Spring Security)
        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());

        // Gerar um token JWT válido para que o frontend possa fazer requisições autenticadas
        String token = jwtUtil.generateToken(userDetails);

        logger.info("Usuário registrado com sucesso: {}", usuario.getEmail());

        // Retornar token, email e role para o frontend
        return new JwtResponseDTO(token, usuario.getEmail(), usuario.getRole().name());
    }

    /**
     * Autentica um usuário existente e gera um token JWT.
     * <p>
     * Fluxo:
     * 1. Valida as credenciais (email + senha) usando o AuthenticationManager do Spring Security
     * 2. Se as credenciais forem inválidas, o Spring lança BadCredentialsException automaticamente
     * 3. Se forem válidas, carrega os dados do usuário
     * 4. Gera um token JWT assinado com a chave secreta
     * 5. Retorna o token para o frontend
     * <p>
     * O que é AuthenticationManager?
     * → Componente central do Spring Security que verifica se email+senha estão corretos
     * → Compara a senha fornecida com o hash armazenado no banco usando BCrypt
     *
     * @param dto Objeto com email e senha do usuário
     * @return JwtResponseDTO contendo o token JWT
     * @throws BadCredentialsException se email ou senha estiverem incorretos
     */
    public JwtResponseDTO login(LoginRequestDTO dto) {
        logger.info("Tentativa de login para o e-mail: {}", dto.email());

        // Autenticar: o AuthenticationManager valida email+senha
        // Se não for válido, lança BadCredentialsException
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.senha())
        );

        // Carregar dados do usuário para criar o token
        UserDetails userDetails = userDetailsService.loadUserByUsername(dto.email());

        // Gerar um novo token JWT assinado
        String token = jwtUtil.generateToken(userDetails);

        // Buscar os dados completos do usuário no banco (para retornar email e role)
        Usuario usuario = usuarioRepository.findByEmail(dto.email()).orElseThrow();

        logger.info("Login realizado com sucesso: {}", usuario.getEmail());

        // Retornar token, email e role para o frontend
        return new JwtResponseDTO(token, usuario.getEmail(), usuario.getRole().name());
    }
}
