import {Component, inject, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import {AuthService} from '../../services/auth.service';
import {LoginRequest} from '../../models/auth.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {

  readonly erro = signal<string | null>(null);
  readonly loading = signal(false);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required]]
  });

  onSubmit(): void {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.erro.set(null);

    this.authService.login(this.form.getRawValue() as LoginRequest).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: err => {
        this.erro.set(err.error?.error ?? 'Erro ao fazer login.');
        this.loading.set(false);
      }
    });
  }
}
