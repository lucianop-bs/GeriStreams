/**
 * login.component.ts — Componente de Autenticação (Login)
 *
 * Responsabilidade: Exibir formulário de login e autenticar o usuário.
 *
 * Gestão de Estado com Signals:
 * - erro: signal<string | null> para mensagens de erro reativas
 * - loading: signal<boolean> para controle de carregamento
 * - Signals atualizam o template automaticamente via .set()
 *
 * Injeção de Dependência com inject():
 * - Padrão moderno do Angular que substitui constructor injection
 * - Permite declarar dependências como campos readonly
 */

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

  /**
   * erro: WritableSignal — mensagem de erro exibida no template
   *
   * null = sem erro | string = mensagem visível
   * Uso no template: @if (erro()) { <div class="alert">{{ erro() }}</div> }
   */
  readonly erro = signal<string | null>(null);
  /**
   * loading: WritableSignal — controla estado de carregamento
   *
   * true = requisição em andamento (botão desabilitado + spinner)
   * false = pronto para nova tentativa
   */
  readonly loading = signal(false);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  /** Formulário reativo com validações de email e senha */
  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required]]
  });

  /**
   * onSubmit() — Processa tentativa de login
   *
   * Fluxo:
   * 1. Valida formulário (se inválido, retorna)
   * 2. Ativa loading e limpa erro anterior via .set()
   * 3. Chama authService.login() com credenciais
   * 4. Sucesso: navega para /dashboard (token já armazenado pelo AuthService)
   * 5. Erro: exibe mensagem e desativa loading
   */
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
