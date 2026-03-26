/**
 * navbar.component.ts — Componente de Navegação (Menu)
 *
 * Responsabilidade: Exibir barra de navegação com links e botão logout.
 *
 * Reatividade via Signals:
 * - isAdmin: computed signal que combina loggedIn() e isAdmin()
 * - Atualiza automaticamente quando o estado de autenticação muda
 * - Sem necessidade de ngOnInit ou subscrições manuais
 */

import {Component, computed, inject} from '@angular/core';
import {RouterModule} from '@angular/router';
import {AuthService} from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './navbar.component.html'
})
export class NavbarComponent {

  private readonly authService = inject(AuthService);

  /**
   * isAdmin: Signal<boolean> computado
   *
   * Combina dois estados reativos:
   * - authService.loggedIn(): se o usuário está autenticado
   * - authService.isAdmin(): se a role é 'ADMIN'
   *
   * computed() recalcula automaticamente quando loggedIn() muda,
   * atualizando o template sem precisar de ngOnInit ou ChangeDetectorRef.
   *
   * Uso no template: @if (isAdmin()) { <a routerLink="/admin">Admin</a> }
   */
  readonly isAdmin = computed(() => this.authService.loggedIn() && this.authService.isAdmin());

  /**
   * logout() — Remove sessão do usuário
   *
   * Delega para AuthService que:
   * 1. Remove token/role do localStorage
   * 2. Atualiza signal loggedIn para false
   * 3. Redireciona para /login
   */
  logout(): void {
    this.authService.logout();
  }
}
