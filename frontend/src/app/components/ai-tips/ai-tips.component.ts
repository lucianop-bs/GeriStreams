/**
 * ai-tips.component.ts — Componente de Dicas com IA
 *
 * Responsabilidade: Exibir dicas personalizadas geradas pelo Claude AI.
 *
 * Gestão de Estado com Signals:
 * - signal() para estado mutável: dicas, loading, erro, gerado
 * - computed() para estado derivado: dicasHtml (converte Markdown → HTML)
 * - Signals eliminam a necessidade de ChangeDetectorRef
 * - Toda mudança via .set() dispara atualização automática do template
 *
 * Fluxo:
 * 1. Usuário clica em "Gerar dicas"
 * 2. API consulta Claude AI com dados financeiros do usuário
 * 3. Claude retorna análise em Markdown
 * 4. computed() dicasHtml converte Markdown para HTML automaticamente
 * 5. Template renderiza com [innerHTML]
 */

import {Component, computed, inject, signal} from '@angular/core';
import {AiService} from '../../services/ai.service';
import {marked} from 'marked';

@Component({
  selector: 'app-ai-tips',
  standalone: true,
  imports: [],
  templateUrl: './ai-tips.component.html'
})
export class AiTipsComponent {

  /** Markdown retornado pela IA (null = ainda não gerado) */
  readonly dicas = signal<string | null>(null);

  // ============ ESTADO REATIVO (Signals) ============
  /** Indica se a requisição à IA está em andamento */
  readonly loading = signal(false);
  /** Mensagem de erro se a requisição falhar */
  readonly erro = signal<string | null>(null);
  /** Flag indicando se as dicas já foram geradas ao menos uma vez */
  readonly gerado = signal(false);
  /**
   * dicasHtml: Signal<string> computado
   *
   * Converte o Markdown do signal dicas() para HTML usando a biblioteca marked.
   * computed() recalcula automaticamente quando dicas() muda.
   *
   * Suporte a Markdown: títulos, negrito, listas, código, citações, links.
   * Resultado é usado no template com [innerHTML]="dicasHtml()"
   */
  readonly dicasHtml = computed(() => {
    const d = this.dicas();
    if (!d) return '';
    return marked.parse(d, {async: false}) as string;
  });

  // ============ ESTADO DERIVADO (Computed Signal) ============
  private readonly aiService = inject(AiService);

  // ============ MÉTODOS ============

  /**
   * gerarDicas() — Solicita dicas personalizadas da IA
   *
   * Fluxo:
   * 1. Ativa loading e limpa estado anterior via signal.set()
   * 2. GET /api/ai/dicas
   * 3. Sucesso: armazena markdown em dicas(), marca como gerado
   * 4. Erro: armazena mensagem em erro()
   * 5. computed() dicasHtml recalcula automaticamente
   */
  gerarDicas(): void {
    this.loading.set(true);
    this.dicas.set(null);
    this.erro.set(null);

    this.aiService.gerarDicas().subscribe({
      next: res => {
        this.dicas.set(res.dicas);
        this.loading.set(false);
        this.gerado.set(true);
      },
      error: err => {
        this.erro.set(err.error?.error ?? 'Não foi possível gerar as dicas. Tente novamente.');
        this.loading.set(false);
      }
    });
  }
}
