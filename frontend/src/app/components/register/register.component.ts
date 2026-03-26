/**
 * register.component.ts — Componente de Registro (Cadastro)
 *
 * Responsabilidade: Exibir formulário de cadastro e registrar novo usuário.
 *
 * Gestão de Estado com Signals:
 * - erro e loading como WritableSignal, atualizados via .set()
 * - Signals disparam atualização automática do template
 *
 * Diferença do Login:
 * - Login: usuário já existe, apenas autentica
 * - Register: cria usuário novo com dados completos (nome, email, senha, salário)
 * - Após registro bem-sucedido, servidor retorna token JWT (já autenticado)
 */

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

  /** Mensagem de erro reativa (null = sem erro) */
  readonly erro = signal<string | null>(null);
  /** Flag de carregamento reativa (desabilita botão enquanto envia) */
  readonly loading = signal(false);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  /**
   * Formulário reativo com 4 campos e suas validações:
   * - nome: obrigatório, mínimo 2 caracteres
   * - email: obrigatório, formato email válido
   * - senha: obrigatório, mínimo 6 caracteres
   * - salario: obrigatório, mínimo R$ 0.01
   */
  readonly form = this.fb.group({
    nome: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(6)]],
    salario: [null as number | null, [Validators.required, Validators.min(0.01)]]
  });

  /**
   * onSubmit() — Processa tentativa de cadastro
   *
   * Fluxo:
   * 1. Valida formulário (se inválido, retorna)
   * 2. Ativa loading e limpa erro anterior via signal.set()
   * 3. Chama authService.register() com dados do formulário
   * 4. Sucesso: navega para /dashboard (token já armazenado pelo AuthService)
   * 5. Erro: exibe mensagem (ex: "Email já cadastrado") e desativa loading
   */
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
