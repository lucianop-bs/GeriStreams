import {Component, inject, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import {AuthService} from '../../services/auth.service';
import {RegisterRequest} from '../../models/auth.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterModule],
  templateUrl: './register.component.html'
})
export class RegisterComponent {

  readonly erro = signal<string | null>(null);
  readonly loading = signal(false);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.group({
    nome: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(6)]],
    salario: [null as number | null, [Validators.required, Validators.min(0.01)]]
  });

  onSubmit(): void {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.erro.set(null);

    this.authService.register(this.form.getRawValue() as RegisterRequest).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: err => {
        this.erro.set(err.error?.error ?? 'Erro ao cadastrar.');
        this.loading.set(false);
      }
    });
  }
}
