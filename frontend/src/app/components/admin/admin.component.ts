import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { NavbarComponent } from '../navbar/navbar.component';
import { UsuarioService } from '../../services/usuario.service';
import { RelatorioService, RankingAssinatura } from '../../services/relatorio.service';
import { Usuario } from '../../models/usuario.model';
import { AssinaturaResponse } from '../../models/assinatura.model';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, NavbarComponent, CurrencyPipe],
  templateUrl: './admin.component.html'
})
export class AdminComponent implements OnInit {

  usuarios: Usuario[] = [];
  assinaturasSelecionadas: AssinaturaResponse[] = [];
  usuarioSelecionado: Usuario | null = null;
  ranking: RankingAssinatura[] = [];
  loading = true;
  loadingPromover = false;

  constructor(
    private usuarioService: UsuarioService,
    private relatorioService: RelatorioService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.usuarioService.listarTodos().subscribe(u => {
      this.usuarios = u;
      this.loading = false;
      this.cdr.detectChanges()
    });
    this.relatorioService.rankingServicos().subscribe(r => (this.ranking = r));
  }

  verAssinaturas(usuario: Usuario): void {
    this.usuarioSelecionado = usuario;
    this.usuarioService.listarAssinaturasDoUsuario(usuario.id).subscribe(a => {
      this.assinaturasSelecionadas = a;
      this.cdr.detectChanges()
    });
  }

  // UC15
  promover(usuario: Usuario): void {
    if (!confirm(`Promover ${usuario.nome} para ADMIN?`)) return;
    this.loadingPromover = true;
    this.usuarioService.promoverParaAdmin(usuario.id).subscribe({
      next: atualizado => {
        const idx = this.usuarios.findIndex(u => u.id === atualizado.id);
        if (idx !== -1) this.usuarios[idx] = atualizado;
        if (this.usuarioSelecionado?.id === atualizado.id) this.usuarioSelecionado = atualizado;
        this.loadingPromover = false;
      },
      error: err => {
        alert(err.error?.error ?? 'Erro ao promover usuário.');
        this.loadingPromover = false;
      }
    });
  }

  get totalGastoPorUsuario(): number {
    return this.assinaturasSelecionadas
      .filter(a => a.ativo)
      .reduce((sum, a) => sum + a.valor, 0);
  }
}
