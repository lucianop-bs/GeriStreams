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

  readonly usuarios = signal<Usuario[]>([]);
  readonly usuarioSelecionado = signal<Usuario | null>(null);
  readonly assinaturasSelecionadas = signal<AssinaturaResponse[]>([]);
  readonly ranking = signal<RankingAssinatura[]>([]);
  readonly loading = signal(true);
  readonly loadingPromover = signal(false);

  readonly totalGastoPorUsuario = computed(() =>
    this.assinaturasSelecionadas()
      .filter(a => a.ativo)
      .reduce((sum, a) => sum + a.valor, 0)
  );

  private readonly usuarioService = inject(UsuarioService);
  private readonly relatorioService = inject(RelatorioService);

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

  verAssinaturas(usuario: Usuario): void {
    this.usuarioSelecionado.set(usuario);
    this.usuarioService.listarAssinaturasDoUsuario(usuario.id).subscribe(a =>
      this.assinaturasSelecionadas.set(a)
    );
  }

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

  labelCategoria(value: string): string {
    return CATEGORIAS.find(c => c.value === value)?.label ?? value;
  }
}
