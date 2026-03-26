/**
 * dashboard.component.ts — Painel Principal do Usuário
 *
 * Responsabilidade: Exibir resumo financeiro, análise de gastos e dicas da IA.
 *
 * Gestão de Estado com Signals:
 * - signal() para estado mutável (usuario, resumo, loading, editandoSalario)
 * - computed() para estado derivado (percentualClass, categoriasEntries)
 * - Signals eliminam a necessidade de ChangeDetectorRef
 * - Toda mudança via .set() ou .update() dispara atualização automática do template
 *
 * Injeção de Dependência com inject():
 * - Padrão moderno Angular que substitui constructor injection
 * - Dependências declaradas como campos readonly no corpo da classe
 *
 * Use Cases:
 * - UC17: Exportar relatório em PDF
 */

import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {CurrencyPipe, DecimalPipe} from '@angular/common';
import {RouterModule} from '@angular/router';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {AssinaturaService} from '../../services/assinatura.service';
import {UsuarioService} from '../../services/usuario.service';
import {RelatorioService} from '../../services/relatorio.service';
import {ResumoFinanceiro} from '../../models/financeiro.model';
import {CATEGORIAS} from '../../models/assinatura.model';
import {AtualizarSalario, Usuario} from '../../models/usuario.model';
import {AiTipsComponent} from '../ai-tips/ai-tips.component';
import {NavbarComponent} from '../navbar/navbar.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    RouterModule,
    ReactiveFormsModule,
    CurrencyPipe,
    DecimalPipe,
    AiTipsComponent,
    NavbarComponent
  ],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {

  /** Dados do usuário logado */
  readonly usuario = signal<Usuario | null>(null);
  /** Resumo financeiro (gastos, percentuais, categorias) */
  readonly resumo = signal<ResumoFinanceiro | null>(null);
  /** Flag de modo edição de salário */
  readonly editandoSalario = signal(false);
  /** Indica carregamento inicial dos dados */
  readonly loading = signal(true);

  // ============ ESTADO REATIVO (Signals) ============
  /**
   * percentualClass: Signal<string> computado
   *
   * Retorna classe CSS baseada no percentual de comprometimento do salário:
   * - 'success' (verde):  < 15% — gasto saudável
   * - 'warning' (amarelo): 15-30% — atenção
   * - 'danger' (vermelho): > 30% — crítico
   *
   * computed() recalcula automaticamente quando resumo() muda.
   * Uso no template: [class]="'text-' + percentualClass()"
   */
  readonly percentualClass = computed(() => {
    const p = this.resumo()?.percentualDoSalario ?? 0;
    if (p >= 30) return 'danger';
    if (p >= 15) return 'warning';
    return 'success';
  });
  /**
   * categoriasEntries: Signal computado que converte gastosPorCategoria
   * de objeto { chaveEnum: valor } para array com labels legíveis, ordenado decrescente.
   *
   * computed() recalcula automaticamente quando resumo() muda.
   * Converte o enum (ex: STREAMING_VIDEO) para label (ex: Streaming de Vídeo)
   * usando a constante CATEGORIAS como mapa de tradução.
   */
  readonly categoriasEntries = computed(() => {
    const r = this.resumo();
    if (!r) return [];
    return Object.entries(r.gastosPorCategoria)
      .map(([chave, valor]) => ({
        nome: CATEGORIAS.find(c => c.value === chave)?.label ?? chave,
        valor
      }))
      .sort((a, b) => b.valor - a.valor);
  });
  // ============ INJEÇÃO DE DEPENDÊNCIAS ============
  private readonly assinaturaService = inject(AssinaturaService);
  private readonly usuarioService = inject(UsuarioService);
  private readonly relatorioService = inject(RelatorioService);

  // ============ ESTADO DERIVADO (Computed Signals) ============
  private readonly fb = inject(FormBuilder);
  /** Formulário reativo para editar salário */
  readonly salarioForm = this.fb.group({
    salario: [null as number | null, [Validators.required, Validators.min(0.01)]]
  });

  // ============ CICLO DE VIDA ============

  /**
   * ngOnInit — Carrega dados iniciais em paralelo
   *
   * Duas chamadas independentes à API:
   * 1. carregarDados(): busca perfil do usuário
   * 2. carregarResumo(): busca resumo financeiro
   */
  ngOnInit(): void {
    this.carregarDados();
    this.carregarResumo();
  }

  // ============ MÉTODOS ============

  /**
   * carregarDados() — Busca perfil do usuário logado
   *
   * GET /api/users/me → armazena no signal usuario
   * Preenche salarioForm com salário atual para edição futura
   */
  carregarDados(): void {
    this.loading.set(true);
    this.usuarioService.buscarPerfil().subscribe(u => {
      this.usuario.set(u);
      this.salarioForm.patchValue({ salario: u.salario });
    });
  }

  /**
   * carregarResumo() — Busca resumo financeiro do usuário
   *
   * GET /api/subscriptions/resumo → armazena no signal resumo
   * Ao completar, desativa loading (página pronta para exibição)
   * computed() percentualClass e categoriasEntries recalculam automaticamente
   */
  carregarResumo(): void {
    this.assinaturaService.resumoFinanceiro().subscribe(r => {
      this.resumo.set(r);
      this.loading.set(false);
    });
  }

  /**
   * salvarSalario() — Atualiza salário do usuário na API
   *
   * Fluxo:
   * 1. Valida formulário
   * 2. PUT /api/users/me/salario com novo valor
   * 3. Atualiza signal usuario com resposta
   * 4. Fecha modo edição via editandoSalario.set(false)
   * 5. Recarrega dados para recalcular percentuais
   */
  salvarSalario(): void {
    if (this.salarioForm.invalid) return;
    this.usuarioService.atualizarSalario(this.salarioForm.getRawValue() as AtualizarSalario).subscribe(u => {
      this.usuario.set(u);
      this.editandoSalario.set(false);
      this.carregarResumo();
    });
  }

  /**
   * baixarRelatorio() — Exporta relatório em PDF [UC17]
   *
   * Fluxo:
   * 1. GET /api/reports/pdf (retorna Blob binário)
   * 2. Cria URL temporária com URL.createObjectURL()
   * 3. Cria elemento <a> dinâmico e simula clique (força download)
   * 4. Libera URL temporária com URL.revokeObjectURL()
   */
  baixarRelatorio(): void {
    this.relatorioService.exportarPdf().subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `relatorio-geristreams-${new Date().toISOString().slice(0, 10)}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }
}
