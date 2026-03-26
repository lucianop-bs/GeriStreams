/**
 * admin.guard.ts — Guard de Autorização de ADMIN
 *
 * Este guard verifica dois níveis de segurança:
 * 1. Autenticação: usuário tem token JWT válido?
 * 2. Autorização: usuário tem role = 'ADMIN'?
 *
 * Diferença entre Autenticação e Autorização:
 * - Autenticação: "Você é quem diz ser?" (validar identidade)
 * - Autorização: "Você tem permissão para isso?" (validar permissões)
 *
 * Exemplo:
 * - João faz login com senha correta → Autenticado (authGuard passa)
 * - João tenta acessar /admin com role='USER' → Não Autorizado (adminGuard bloqueia)
 *
 * Nota: Este guard é usado JUNTO com authGuard, não substitui
 * Na rota: canActivate: [authGuard, adminGuard]
 * - Primeiro: authGuard verifica autenticação
 * - Depois: adminGuard verifica se é admin
 */

import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {AuthService} from '../services/auth.service';

/**
 * adminGuard — Guard que verifica se usuário é ADMIN
 *
 * Pré-requisitos (para este guard passar):
 * 1. Deve ter token JWT (authService.getToken() === true)
 * 2. Deve ter role === 'ADMIN' (authService.isAdmin() === true)
 *
 * Verificação lógica:
 * ```
 * if (token EXISTE && isAdmin) {
 *   ✓ Permite acessar /admin
 * } else {
 *   ✗ Redireciona para /dashboard
 * }
 * ```
 *
 * Fluxo de uma tentativa de acesso não autorizado:
 * 1. João (USER) tenta acessar localhost:4200/admin
 * 2. Angular executa authGuard → PASSA (tem token)
 * 3. Angular executa adminGuard → FALHA (role != 'ADMIN')
 * 4. adminGuard redireciona para /dashboard
 * 5. João vê dashboard, não vê painel admin
 *
 * Segurança:
 * - localStorage pode ser manipulado pelo usuário (ou hacker)
 * - Se João editar localStorage e mudar role para ADMIN, o guard passa
 * - MAS: o servidor ainda valida role antes de retornar dados sensíveis
 * - Backend NUNCA confia no frontend, sempre valida de novo
 * - O guard existe para UX (melhor experiência), não para segurança real
 */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Verifica DOIS requisitos:
  // 1. authService.getToken(): tem JWT token armazenado?
  // 2. authService.isAdmin(): role === 'ADMIN'?
  // Ambos devem ser true (operador &&)
  if (authService.getToken() && authService.isAdmin()) {
    // Ambas as condições passaram → usuário pode acessar /admin
    return true;
  }

  // Uma ou ambas as condições falharam
  // Redireciona para /dashboard em vez de /login
  // Motivo: usuário já está autenticado, mas não tem permissão
  // Mostrar /login seria confuso (já fez login)
  return router.createUrlTree(['/dashboard']);
};
