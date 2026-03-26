/**
 * auth.guard.ts — Guard de Autenticação
 *
 * Um "Guard" é uma função que o Angular chama ANTES de ativar uma rota.
 * Ela decide se o usuário tem permissão para acessar aquela página.
 *
 * CONCEITO — Por que usar Guards?
 * Sem guard:
 *   - Usuário acessa localhost:4200/dashboard sem estar logado
 *   - Página carrega
 *   - Componente tenta fazer requisição protegida
 *   - Servidor retorna 401 (Unauthorized)
 *   - Componente fica em estado quebrado
 *
 * Com guard:
 *   - Usuário acessa localhost:4200/dashboard
 *   - ANTES do componente carregar, authGuard é executado
 *   - Se não tem token: guard redireciona para /login
 *   - Se tem token: componente carrega normalmente
 *   - Resultado: navegação mais segura e fluxo previsível
 */

import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {AuthService} from '../services/auth.service';

/**
 * authGuard — Guard que verifica se usuário está autenticado (tem token JWT)
 *
 * Parâmetro implícito:
 * O Angular passa automaticamente (via ActivatedRouteSnapshot e RouterStateSnapshot),
 * mas nós não precisamos deles aqui. Podemos ignorar.
 *
 * Retorno:
 * - true: Usuário autorizado, ativa a rota normalmente
 * - false: Usuário não autorizado, ignora a navegação
 * - UrlTree: Redireciona para URL específica (nosso caso)
 *
 * Fluxo:
 * 1. Angular ativa a rota /dashboard
 * 2. Antes de renderizar, chama authGuard
 * 3. Guard injeta AuthService e Router
 * 4. Verifica if (authService.getToken())
 * 5. Se tem token → return true (prossegue com a rota)
 * 6. Se NÃO tem → router.createUrlTree(['/login']) (redireciona)
 *
 * Uso em app.routes.ts:
 * {
 *   path: 'dashboard',
 *   canActivate: [authGuard],  // Protege esta rota
 *   loadComponent: () => import(...).then(m => m.DashboardComponent)
 * }
 */
export const authGuard: CanActivateFn = () => {
  // Dependency Injection (DI) usando inject()
  // Sintaxe moderna do Angular 16+
  // Na verdade, pede ao Angular: "Me dê uma instância de AuthService"
  const authService = inject(AuthService);

  // Injeta o Router para fazer navegação programática
  const router = inject(Router);

  // Verifica se JWT token existe no localStorage
  if (authService.getToken()) {
    // Token existe → usuário está autenticado
    // Permite ativar a rota
    return true;
  }

  // Token não existe → usuário não autenticado
  // createUrlTree() cria um objeto UrlTree que redireciona
  // Resultado: usuário é enviado para /login automaticamente
  return router.createUrlTree(['/login']);
};
