/**
 * error.interceptor.ts — Interceptador que trata erros HTTP globalmente
 *
 * Este interceptador captura TODAS as respostas de erro da API e decide
 * como lidar com cada um, baseado no código de erro HTTP.
 *
 * Códigos de erro tratados:
 * - 401 Unauthorized: Token inválido/expirado → fazer logout automático
 * - 403 Forbidden: Token válido mas sem permissão → redirecionar para dashboard
 * - Outros: passar o erro adiante para o componente tratar
 *
 * RxJS Operators:
 * - pipe(): conecta operadores (como um "tubo" de transformação)
 * - catchError(): "apanha" erros que ocorrem na requisição
 * - throwError(): relança o erro para que componentes possam tratá-lo
 */

import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {Router} from '@angular/router';
import {catchError, throwError} from 'rxjs';
import {AuthService} from '../services/auth.service';

/**
 * errorInterceptor — Trata erros HTTP centralizadamente
 *
 * Fluxo de uma requisição com erro:
 * 1. Componente chama: this.http.get('/api/users')
 * 2. Servidor retorna erro (ex: 401, 403, 500)
 * 3. ANTES de chegar no componente, errorInterceptor intercepta
 * 4. Interceptador analisa error.status
 * 5. Se 401: faz logout automático
 * 6. Se 403: redireciona para dashboard
 * 7. Relança o erro para o componente tratar
 *
 * Benefício:
 * - Lógica de erro centralizada (um único lugar)
 * - Não precisa tratar 401 em cada componente
 * - Comportamento consistente em toda a app
 *
 * Segurança:
 * - 401: Token expirado ou inválido
 *   → Não faz sentido o usuário continuar logado
 *   → Fazer logout e redirecionar para login
 * - 403: Token válido mas sem permissão
 *   → Usuário não tem acesso aquele recurso
 *   → Redirecionar para dashboard é "amigo demais"
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  // Injeta dependências
  const router = inject(Router);
  const authService = inject(AuthService);

  // Executa a requisição com next()
  // pipe() conecta operadores RxJS que processam a resposta
  return next(req).pipe(
    // catchError() intercepta erros que ocorrem durante a requisição
    // error: objeto com propriedades como status, message, etc
    catchError(error => {
      // Verifica código de erro HTTP (401 = Unauthorized)
      if (error.status === 401) {
        // Token inválido/expirado
        // Fazer logout (remove token, emite false no BehaviorSubject)
        authService.logout();
        // O logout() já redireciona para /login, não precisa fazer nada mais
      }

      // Verifica código de erro HTTP (403 = Forbidden)
      if (error.status === 403) {
        // Token válido mas sem permissão para acessar recurso
        // Redireciona para dashboard em vez de sair da sessão
        router.navigate(['/dashboard']);
      }

      // Relança o erro para que o componente possa tratá-lo
      // throwError() permite que subscribe() veja o erro
      // Exemplo:
      // this.http.get('/api/users').subscribe({
      //   next: data => console.log(data),
      //   error: err => console.log(err) // Aqui recebe o erro
      // });
      return throwError(() => error);
    })
  );
};
