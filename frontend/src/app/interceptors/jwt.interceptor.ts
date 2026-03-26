/**
 * jwt.interceptor.ts — Interceptador que adiciona JWT token em requisições
 *
 * Um Interceptador é uma função que "intercepta" todas as requisições HTTP
 * ANTES de serem enviadas ao servidor.
 *
 * CONCEITO — Por que usar interceptador?
 *
 * Sem interceptador (problema):
 * ```typescript
 * // Em cada serviço, precisaria fazer isso:
 * const headers = new HttpHeaders({
 *   'Authorization': `Bearer ${this.authService.getToken()}`
 * });
 * this.http.get('/api/users', { headers });
 * this.http.post('/api/subscriptions', data, { headers });
 * this.http.put('/api/users/salary', data, { headers });
 * // Repetir em TODA requisição protegida... tedioso!
 * ```
 *
 * Com interceptador (solução):
 * ```typescript
 * // Interceptador adiciona token AUTOMATICAMENTE
 * this.http.get('/api/users');         // Token adicionado automaticamente
 * this.http.post('/api/subscriptions', data); // Token adicionado automaticamente
 * ```
 *
 * Fluxo de uma requisição com interceptador:
 * 1. Componente chama: this.http.get('/api/users')
 * 2. ANTES de sair, jwtInterceptor intercepta
 * 3. Interceptador pega token do localStorage
 * 4. Interceptador clona a requisição e adiciona header Authorization
 * 5. Requisição sai com o header Authorization preenchido
 * 6. Servidor recebe requisição com token
 * 7. Servidor valida token e processa requisição
 */

import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {AuthService} from '../services/auth.service';

/**
 * jwtInterceptor — Intercepta todas as requisições e adiciona token JWT
 *
 * Parâmetros:
 * @param req: A requisição HTTP original (objeto imutável)
 * @param next: Função que passa a requisição adiante na cadeia
 *
 * Retorno:
 * Observable da resposta HTTP após processar a requisição
 *
 * Fluxo:
 * 1. Pega token do localStorage via AuthService
 * 2. Se token existe:
 *    - Clona a requisição original (não modifica original, cria nova)
 *    - Adiciona header Authorization: "Bearer {token}"
 * 3. Se token não existe:
 *    - Deixa requisição como está
 * 4. Passa requisição para o próximo middleware (next())
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  // Injeta AuthService para pegar o token
  const token = inject(AuthService).getToken();

  // Verifica se tem token armazenado
  if (token) {
    // Se tem token, clona a requisição e adiciona o header Authorization
    // req.clone() cria uma CÓPIA da requisição (não modifica original)
    // setHeaders: adiciona (ou substitui) headers na requisição clonada
    // Authorization: "Bearer {token}" é o padrão OAuth 2.0
    //   - "Bearer" = tipo de token (padrão para JWT)
    //   - {token} = o JWT token real (ex: "eyJhbGciOi...")
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  // Passa a requisição modificada para o próximo handler
  // next() executa o resto da cadeia de interceptadores
  // Exemplo: se houver outro interceptador depois deste, ele é executado agora
  return next(req);
};
