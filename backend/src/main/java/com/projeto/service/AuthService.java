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

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

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

    public JwtResponseDTO registrar(RegisterRequestDTO dto) {
        logger.info("Tentativa de registro para o e-mail: {}", dto.email());

        if (usuarioRepository.existsByEmail(dto.email())) {
            logger.warn("Registro rejeitado: e-mail já cadastrado - {}", dto.email());
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setSalario(dto.salario());

        usuarioRepository.save(usuario);

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        logger.info("Usuário registrado com sucesso: {}", usuario.getEmail());

        return new JwtResponseDTO(token, usuario.getEmail(), usuario.getRole().name());
    }

    public JwtResponseDTO login(LoginRequestDTO dto) {
        logger.info("Tentativa de login para o e-mail: {}", dto.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.senha())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(dto.email());
        String token = jwtUtil.generateToken(userDetails);

        Usuario usuario = usuarioRepository.findByEmail(dto.email()).orElseThrow();

        logger.info("Login realizado com sucesso: {}", usuario.getEmail());

        return new JwtResponseDTO(token, usuario.getEmail(), usuario.getRole().name());
    }
}
