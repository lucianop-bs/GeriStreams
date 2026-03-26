package com.projeto.security;

import com.projeto.model.Usuario;
import com.projeto.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço de Detalhes de Usuário (Spring Security Integration).
 * <p>
 * Este serviço implementa UserDetailsService, interface do Spring Security.
 * <p>
 * O que é UserDetailsService?
 * → Interface central do Spring Security para carregar usuários do banco de dados
 * → Usada em múltiplos lugares:
 * 1. DaoAuthenticationProvider (durante login)
 * 2. JwtAuthFilter (para validar token e carregar usuário)
 * 3. Qualquer lugar que precise dos dados do usuário
 * <p>
 * Por que não usar Usuario diretamente?
 * → Usuario é entidade JPA (data + mapeamento ORM)
 * → UserDetails é interface Spring Security (autenticação + autorização)
 * → Conversão: Usuario (do banco) → UserDetails (para Spring)
 * <p>
 * UserDetails: contém
 * → username (aqui é o email)
 * → password (hash em BCrypt)
 * → authorities (papéis: ROLE_USER, ROLE_ADMIN)
 * → NÃO contém: salário, createdAt, etc (apenas dados de autenticação)
 *
 * @Service: Spring instancia automaticamente
 * @implementa UserDetailsService: obrigação de implementar loadUserByUsername()
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Carrega um usuário pelo email (username).
     * <p>
     * Implementação obrigatória de UserDetailsService.
     * <p>
     * Quando é chamado?
     * → DaoAuthenticationProvider.authenticate(): durante login
     * Fluxo: username + password → AuthenticationManager → DaoAuthenticationProvider
     * → Provider chama loadUserByUsername(username) para buscar dados
     * → JwtAuthFilter.doFilterInternal(): ao validar JWT
     * Fluxo: JWT válido → extrai email → loadUserByUsername(email) → carrega dados
     * → Qualquer código que precise dos dados do usuário
     * <p>
     * Fluxo:
     * 1. Recebe o email (username no contexto do Spring)
     * 2. Busca no banco via UsuarioRepository.findByEmail()
     * 3. Se não encontrar: lança UsernameNotFoundException (padrão Spring)
     * 4. Se encontrar: converte Usuario → UserDetails
     * <p>
     * Conversão Usuario → UserDetails:
     * → Usuario (entidade JPA): id, nome, email, senha, salario, role, createdAt, assinaturas
     * → UserDetails (Spring): username, password, authorities
     * → Descarta: nome, salario, createdAt, assinaturas (não são usados em autenticação)
     * <p>
     * Por que lançar UsernameNotFoundException?
     * → É a exceção esperada pelo Spring Security
     * → Sem isto, Spring não saberia como tratar o erro
     * → Spring detecta e retorna 401 Unauthorized para o cliente
     * <p>
     * Por que "username" quando são emails?
     * → Convenção do Spring: field "username" pode conter qualquer identificador único
     * → Aqui usamos email como identificador (mais seguro que ID numérico)
     * → Nomes: "username" vem de sistemas legados, aceita emails também
     *
     * @param email Email do usuário a carregar
     * @return UserDetails com dados de autenticação do usuário
     * @throws UsernameNotFoundException se usuário não encontrado
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Busca o usuário no banco pelo email
        Usuario usuario = usuarioRepository.findByEmail(email)
                // Se não encontrar (Optional vazio):
                // .orElseThrow(() -> lança exceção):
                // UsernameNotFoundException é o padrão do Spring Security
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        // Converte Usuario (JPA) → User (Spring Security UserDetails)
        // new User(...): classe concreta do Spring que implementa UserDetails
        return new User(
                // username: email do usuário (identificador único)
                usuario.getEmail(),

                // password: senha em hash BCrypt (NUNCA em texto puro!)
                // Exemplo: "$2a$10$yVm7RKD3yVm7RKD3yVm7R..."
                usuario.getSenha(),

                // authorities: papéis/permissões do usuário
                // SimpleGrantedAuthority: classe simples que implementa GrantedAuthority
                // "ROLE_" + usuario.getRole().name():
                //   → usuario.getRole() retorna enum (USER ou ADMIN)
                //   → .name() retorna String: "USER" ou "ADMIN"
                //   → "ROLE_" + "USER" = "ROLE_USER"
                // Convenção Spring: autoridades começam com "ROLE_"
                // Exemplo: @PreAuthorize("hasRole('USER')") procura por "ROLE_USER"
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()))
        );
    }
}
