package com.projeto.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilidade para manipulação de JWT (JSON Web Token).
 * <p>
 * JWT é um padrão moderno de autenticação stateless em aplicações web.
 * <p>
 * O que é JWT?
 * → Token assinado que contém informações do usuário
 * → Formato: header.payload.signature
 * → Exemplo: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2FvQGVtYWlsLmNvbSJ9.signature...
 * → Decodificável no site jwt.io para visualizar o conteúdo
 * <p>
 * Vantagens do JWT:
 * → Stateless: servidor não precisa armazenar sessões (escalável)
 * → Seguro: assinado criptograficamente, não pode ser alterado
 * → Portável: pode ser usado em múltiplos servidores/domínios
 * → Padrão da indústria: OAuth 2.0, muitas APIs usam
 * <p>
 * Fluxo:
 * 1. Usuário faz login (email + senha)
 * 2. Server verifica credenciais (corretas?)
 * 3. Server gera JWT com informações do usuário
 * 4. Server retorna JWT ao cliente
 * 5. Cliente armazena JWT (localStorage, cookie, etc)
 * 6. Client envia JWT em requisições futuras (header Authorization: Bearer <token>)
 * 7. Server valida JWT (assinatura, expiração, etc) e processa requisição
 * 8. Nenhum "session store" necessário no servidor — só a chave secreta
 * <p>
 * Segurança:
 * → Assinado com HMAC-SHA256 (algoritmo criptográfico)
 * → Sem a chave secreta, impossível forjar ou alterar o token
 * → Se alguém tentar mudar os dados (email, role), a assinatura fica inválida
 *
 * @Component: marca como bean Spring (será instanciada e injetada automaticamente)
 */
@Component
public class JwtUtil {

    // @Value injeta a propriedade do application.properties ou .yml
    // ${app.jwt.secret}: lê a chave secreta do arquivo de configuração
    // Esta chave DEVE ser longa, aleatória e sigilosa (produção: muito importante!)
    @Value("${app.jwt.secret}")
    private String secret;

    // Tempo de expiração em milissegundos
    // ${app.jwt.expiration-ms}: exemplo: 86400000 (24 horas)
    // Após expirar, o token é rejeitado, forçando novo login
    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Gera a chave de assinatura a partir da string secreta.
     * <p>
     * Por que precisamos disto?
     * → A biblioteca JJWT precisa de uma SecretKey (classe especializada)
     * → Não pode ser qualquer String, deve ser criptograficamente válida
     * → Keys.hmacShaKeyFor() valida e cria a chave segura
     * <p>
     * StandardCharsets.UTF_8:
     * → Converte a String para bytes usando UTF-8
     * → UTF-8 é o padrão universal (suporta todos os caracteres)
     *
     * @return SecretKey pronta para assinar tokens
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gera um novo JWT para um usuário.
     * <p>
     * Fluxo:
     * 1. Cria um builder do JWT (padrão Builder)
     * 2. Adiciona o "subject" (quem é este token = email do usuário)
     * 3. Adiciona claims (informações adicionais = roles/papéis)
     * 4. Define horário de emissão (agora)
     * 5. Define horário de expiração (agora + 24h, por exemplo)
     * 6. Assina com a chave secreta (HMAC-SHA256)
     * 7. Compacta em uma string (header.payload.signature)
     * 8. Retorna o token
     * <p>
     * Padrão Builder:
     * → Construção fluente (método encadeado)
     * → Cada .método() retorna o builder, permitindo chamar o próximo
     * → Comum em padrões modernos de Java
     * <p>
     * UserDetails:
     * → Interface do Spring Security que representa um usuário autenticado
     * → Contém username (email), password, authorities (roles)
     * → Usamos para extrair dados que vão no JWT
     * <p>
     * Exemplo de resultado:
     * → Token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2FvQGVtYWlsLmNvbSIsInJvbGVzIjoiUk9MRV9VU0VSIn0.xyz..."
     * → Pode ser decodificado em jwt.io para ver o conteúdo
     *
     * @param userDetails Dados do usuário (username, authorities)
     * @return String: token JWT assinado
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                // subject = "o que este token representa"
                // Geralmente é o username (que neste projeto é o email)
                .subject(userDetails.getUsername())

                // claim = "informação adicional"
                // "roles" = papéis do usuário (USER, ADMIN)
                // Usado para validação de autorização
                .claim("roles", userDetails.getAuthorities().toString())

                // issuedAt = "quando foi emitido"
                // Usado para auditorias
                .issuedAt(new Date())

                // expiration = "quando expira"
                // System.currentTimeMillis(): hora atual em ms
                // + expirationMs: adiciona o intervalo de validade
                // Exemplo: agora (1000000 ms) + 86400000 ms (24h) = expira em 1086400000
                .expiration(new Date(System.currentTimeMillis() + expirationMs))

                // signWith = "assina o token"
                // getSigningKey(): usa a chave secreta
                // Sem isto, qualquer um poderia criar tokens falsos!
                .signWith(getSigningKey())

                // compact = "transforma em string"
                // Gera o formato: header.payload.signature
                .compact();
    }

    /**
     * Extrai o email (subject) do token JWT.
     * <p>
     * Fluxo:
     * 1. Faz parse do token (valida assinatura, extrai dados)
     * 2. Obtém o "subject" (que é o email)
     * 3. Retorna como String
     * <p>
     * Quando usar:
     * → Quando você já sabe que o token é válido
     * → Para obter o email do usuário autenticado
     * → Geralmente feito pelo JwtAuthFilter
     *
     * @param token JWT string (pode vir do header Authorization)
     * @return Email do usuário
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Valida se um token é válido para um usuário.
     * <p>
     * Duas validações:
     * 1. Email no token = email do usuário (segurança: alguém não trocou o subject)
     * 2. Token não expirou (segurança temporal)
     * <p>
     * Fluxo:
     * 1. Extrai email do token
     * 2. Compara com o username do UserDetails (são iguais?)
     * 3. Verifica se não está expirado (!isTokenExpired)
     * 4. Retorna true APENAS se ambas as condições forem verdadeiras
     * <p>
     * Por que ambas validações?
     * → Sem a 1ª: alguém poderia roubar um token e usar com outro usuário
     * → Sem a 2ª: tokens antigos continuariam válidos indefinidamente
     * → Combinadas: segurança em múltiplas camadas
     * <p>
     * Quando usar:
     * → No JwtAuthFilter: antes de permitir uma requisição
     * → Sempre que receber um token do cliente
     *
     * @param token       JWT a validar
     * @param userDetails Usuário supostamente dono do token
     * @return true se válido, false se inválido ou expirado
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String email = extractEmail(token);
        // && = AND lógico: ambas devem ser verdadeiras
        // email.equals(...): email no token = email do usuário?
        // !isTokenExpired(...): token não expirou?
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Verifica se um token expirou.
     * <p>
     * Lógica:
     * 1. Extrai a data de expiração do token
     * 2. Compara com agora (new Date())
     * 3. Se expiração.before(agora): token está no passado = expirou
     * 4. Retorna true se expirou, false se ainda é válido
     * <p>
     * Exemplo:
     * → Expiração: 01/01/2025 10:00
     * → Agora: 01/01/2025 11:00
     * → before() = true (expiração está antes de agora)
     * → Token expirou
     *
     * @param token JWT a verificar
     * @return true se expirou, false se ainda válido
     */
    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    /**
     * Faz parse e valida o token JWT.
     * <p>
     * Esta função é CRÍTICA para segurança!
     * <p>
     * Fluxo:
     * 1. Cria um parser (leitor de JWT)
     * 2. .verifyWith(getSigningKey()): CRUCIAL!
     * → Valida a assinatura do token
     * → Verifica que ninguém alterou os dados
     * → Se alguém mudou 1 byte do token, a assinatura falha
     * → Lança SignatureException se inválido
     * 3. .build(): cria o parser com configuração
     * 4. .parseSignedClaims(token): faz parse do token assinado
     * 5. .getPayload(): extrai os dados (claims) do token
     * 6. Retorna os claims (Claims = mapa de dados)
     * <p>
     * Claims:
     * → Estrutura de dados como um Map<String, Object>
     * → Contém: subject, expiration, issuedAt, roles, e qualquer informação adicionada
     * → Pode ser acessado por: claims.getSubject(), claims.getExpiration(), etc
     * <p>
     * Exceções que podem ser lançadas:
     * → JwtException: token malformado
     * → SignatureException: assinatura inválida (alguém alterou!)
     * → ExpiredJwtException: token expirou
     * → o JwtAuthFilter captura estas exceções
     * <p>
     * Por que esta é uma função auxiliar privada?
     * → Só deve ser chamada internamente
     * → Exposição pública poderia quebrar segurança
     * → Controla quem pode fazer parse de tokens
     *
     * @param token JWT string
     * @return Claims: dados extraídos do token
     * @throws JwtException se token inválido ou expirado
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                // SEGURANÇA: valida a assinatura usando a chave secreta
                // Se não temos a chave secreta correta, isto falha
                .verifyWith(getSigningKey())
                .build()
                // parseSignedClaims: parse de um token assinado
                .parseSignedClaims(token)
                // getPayload: obtém os dados (subject, expiration, claims customizados)
                .getPayload();
    }
}
