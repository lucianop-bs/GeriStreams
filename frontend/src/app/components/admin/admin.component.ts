/**
 * admin.component.ts — Painel Administrativo
 *
 * Responsabilidade: Gerenciar usuários e visualizar relatórios da plataforma.
 * Acesso restrito a usuários com role = 'ADMIN' (protegido por AdminGuard).
 *
 * Gestão de Estado com Signals:
 * - signal() para estado mutável: usuarios, ranking, seleção, loading
 * - computed() para estado derivado: totalGastoPorUsuario
 * - .update() para mutações imutáveis de arrays (ex: atualizar usuário na lista)
 * - Elimina necessidade de ChangeDetectorRef (signals auto-rastreados)
 *
 * Use Cases:
 * - UC16: Ranking de serviços mais usados
 * - UC15: Promover usuário para ADMIN
 */

import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {CurrencyPipe} from '@angular/common';
import {UsuarioService} from '../../services/usuario.service';
import {RankingAssinatura, RelatorioService} from '../../services/relatorio.service';
import {Usuario} from '../../models/usuario.model';
import {AssinaturaResponse, CATEGORIAS} from '../../models/assinatura.model';
import {NavbarComponent} from '../navbar/navbar.component';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CurrencyPipe, NavbarComponent],
  templateUrl: './admin.component.html'
})
export class AdminComponent implements OnInit {

  /** Lista de todos os usuários da plataforma */
  readonly usuarios = signal<Usuario[]>([]);
  /** Usuário selecionado na lista (clicado pelo admin) */
  readonly usuarioSelecionado = signal<Usuario | null>(null);

  // ============ ESTADO REATIVO (Signals) ============
  /** Assinaturas do usuário selecionado */
  readonly assinaturasSelecionadas = signal<AssinaturaResponse[]>([]);
  /** Ranking de serviços mais usados [UC16] */
  readonly ranking = signal<RankingAssinatura[]>([]);
  /** Indica carregamento inicial da página */
  readonly loading = signal(true);
  /** Indica carregamento durante promoção de usuário */
  readonly loadingPromover = signal(false);
  /**
   * totalGastoPorUsuario: Signal<number> computado
   *
   * Soma os valores de assinaturas ATIVAS do usuário selecionado.
   * computed() recalcula automaticamente quando assinaturasSelecionadas() muda.
   *
   * Filtra apenas ativas porque assinaturas inativas não geram custo.
   */
  readonly totalGastoPorUsuario = computed(() =>
    this.assinaturasSelecionadas()
      .filter(a => a.ativo)
      .reduce((sum, a) => sum + a.valor, 0)
  );
  private readonly usuarioService = inject(UsuarioService);

  // ============ ESTADO DERIVADO (Computed Signal) ============
  private readonly relatorioService = inject(RelatorioService);

  // ============ CICLO DE VIDA ============

  /**
   * ngOnInit — Carrega dados em paralelo
   *
   * Duas chamadas independentes:
   * 1. Lista de usuários (controla loading da página)
   * 2. Ranking de serviços [UC16]
   */
  ngOnInit(): void {
    this.usuarioService.listarTodos().subscribe(u => {
      this.usuarios.set(u);
      this.loading.set(false);
    });

    this.relatorioService.rankingServicos().subscribe({
      next: r => this.ranking.set(r),
      error: () => this.ranking.set([])
    });
  }

  // ============ MÉTODOS ============

  /**
   * verAssinaturas() — Carrega assinaturas de um usuário específico
   *
   * @param usuario - Usuário clicado na lista pelo admin
   *
   * Atualiza signals usuarioSelecionado e assinaturasSelecionadas.
   * computed() totalGastoPorUsuario recalcula automaticamente.
   */
  verAssinaturas(usuario: Usuario): void {
    this.usuarioSelecionado.set(usuario);
    this.usuarioService.listarAssinaturasDoUsuario(usuario.id).subscribe(a =>
      this.assinaturasSelecionadas.set(a)
    );
  }

  /**
   * promover() — Promove usuário de USER para ADMIN [UC15]
   *
   * @param usuario - Usuário a ser promovido (deve ter role = 'USER')
   *
   * Fluxo:
   * 1. Pede confirmação via confirm()
   * 2. PATCH /api/admin/users/{id}/promote
   * 3. Atualiza lista de usuários com .update() (mutação imutável)
   * 4. Se é o usuário selecionado, atualiza a seleção também
   *
   * .update() recebe a lista atual e retorna uma nova lista com o item atualizado,
   * disparando re-renderização apenas dos itens afetados.
   */
  promover(usuario: Usuario): void {
    if (!confirm(`Promover ${usuario.nome} para ADMIN?`)) return;

    this.loadingPromover.set(true);

    this.usuarioService.promoverParaAdmin(usuario.id).subscribe({
      next: atualizado => {
        this.usuarios.update(list =>
          list.map(u => u.id === atualizado.id ? atualizado : u)
        );

        if (this.usuarioSelecionado()?.id === atualizado.id) {
          this.usuarioSelecionado.set(atualizado);
        }

        this.loadingPromover.set(false);
      },
      error: err => {
        alert(err.error?.error ?? 'Erro ao promover usuário.');
        this.loadingPromover.set(false);
      }
    });
  }

  /** Converte código de categoria (ex: STREAMING_VIDEO) em label legível (ex: Streaming de Vídeo) */
  labelCategoria(value: string): string {
    return CATEGORIAS.find(c => c.value === value)?.label ?? value;
  }
}
