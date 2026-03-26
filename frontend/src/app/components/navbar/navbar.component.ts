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

  readonly isAdmin = computed(() => this.authService.loggedIn() && this.authService.isAdmin());

  logout(): void {
    this.authService.logout();
  }
}
