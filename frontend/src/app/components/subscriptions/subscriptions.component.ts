/**
 * subscriptions.component.ts — Gerenciamento de Assinaturas (CRUD)
 *
 * Responsabilidade: Listar, criar, editar, deletar e ativar/desativar assinaturas.
 *
 * Gestão de Estado com Signals:
 * - signal() para estado mutável: assinaturas, editandoId, mostrarFormulario, loading
 * - .set() para atribuir novos valores, disparando atualização automática do template
 * - Elimina necessidade de ChangeDetectorRef (signals são auto-rastreados)
 *
 * Injeção de Dependência com inject():
 * - AssinaturaService e FormBuilder injetados como campos readonly
 */

import {Component, inject, OnInit, signal} from '@angular/core';
import {CurrencyPipe} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {AssinaturaService} from '../../services/assinatura.service';
import {AssinaturaRequest, AssinaturaResponse, CATEGORIAS} from '../../models/assinatura.model';
import {NavbarComponent} from '../navbar/navbar.component';

@Component({
  selector: 'app-subscriptions',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe, NavbarComponent],
  templateUrl: './subscriptions.component.html'
})
export class SubscriptionsComponent implements OnInit {

  /** Lista de assinaturas do usuário */
  readonly assinaturas = signal<AssinaturaResponse[]>([]);
  /** Constante com categorias disponíveis (não reativo, valor fixo) */
  readonly categorias = CATEGORIAS;

  // ============ ESTADO REATIVO (Signals) ============
  /** ID da assinatura em edição (null = modo criação) */
  readonly editandoId = signal<number | null>(null);
  /** Controla visibilidade do formulário de criação/edição */
  readonly mostrarFormulario = signal(false);
  /** Indica se está salvando/enviando requisição */
  readonly loading = signal(false);
  private readonly assinaturaService = inject(AssinaturaService);
  private readonly fb = inject(FormBuilder);
  /** Formulário reativo para criar/editar assinatura */
  readonly form = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    valor: [null as number | null, [Validators.required, Validators.min(0.01)]],
    categoria: ['', Validators.required]
  });

  // ============ CICLO DE VIDA ============

  /** Carrega assinaturas da API ao inicializar o componente */
  ngOnInit(): void {
    this.carregar();
  }

  // ============ MÉTODOS ============

  /**
   * carregar() — Busca todas as assinaturas do usuário logado
   *
   * GET /api/subscriptions → atualiza signal assinaturas via .set()
   * O template re-renderiza automaticamente ao detectar mudança no signal.
   */
  carregar(): void {
    this.assinaturaService.listar().subscribe(a => this.assinaturas.set(a));
  }

  /**
   * abrirFormulario() — Abre formulário para criar ou editar assinatura
   *
   * @param assinatura - (Opcional) Se fornecido, preenche formulário para edição.
   *                     Se omitido, abre formulário vazio para criação.
   *
   * Atualiza signals mostrarFormulario e editandoId via .set()
   */
  abrirFormulario(assinatura?: AssinaturaResponse): void {
    this.mostrarFormulario.set(true);
    if (assinatura) {
      this.editandoId.set(assinatura.id);
      this.form.patchValue(assinatura);
    } else {
      this.editandoId.set(null);
      this.form.reset();
    }
  }

  /** Fecha formulário e limpa estado de edição */
  fecharFormulario(): void {
    this.mostrarFormulario.set(false);
    this.editandoId.set(null);
    this.form.reset();
  }

  /**
   * salvar() — Cria ou atualiza assinatura via API
   *
   * Decide dinamicamente entre POST (criar) e PUT (atualizar)
   * baseado no valor do signal editandoId():
   * - editandoId() !== null → PUT /api/subscriptions/{id}
   * - editandoId() === null → POST /api/subscriptions
   *
   * Após sucesso: fecha formulário e recarrega lista
   */
  salvar(): void {
    if (this.form.invalid) return;

    this.loading.set(true);
    const payload = this.form.getRawValue() as AssinaturaRequest;

    const request$ = this.editandoId()
      ? this.assinaturaService.atualizar(this.editandoId()!, payload)
      : this.assinaturaService.criar(payload);

    request$.subscribe({
      next: () => {
        this.fecharFormulario();
        this.carregar();
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  /**
   * remover() — Deleta assinatura com confirmação do usuário
   *
   * @param id - ID da assinatura a remover
   * Exibe confirm() nativo antes de enviar DELETE /api/subscriptions/{id}
   */
  remover(id: number): void {
    if (!confirm('Deseja remover esta assinatura?')) return;
    this.assinaturaService.remover(id).subscribe(() => this.carregar());
  }

  /**
   * toggleAtivo() — Alterna estado ativo/inativo de uma assinatura
   *
   * @param id - ID da assinatura
   * PATCH /api/subscriptions/{id}/toggle → servidor inverte o estado
   */
  toggleAtivo(id: number): void {
    this.assinaturaService.toggleAtivo(id).subscribe(() => this.carregar());
  }

  /**
   * labelCategoria() — Converte código de categoria em rótulo legível
   *
   * @param value - Código da categoria (ex: "STREAMING_VIDEO")
   * @returns Rótulo formatado (ex: "Streaming & Vídeo") ou o próprio valor como fallback
   */
  labelCategoria(value: string): string {
    return this.categorias.find(c => c.value === value)?.label ?? value;
  }
}
