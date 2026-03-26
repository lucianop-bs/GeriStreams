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

  readonly usuario = signal<Usuario | null>(null);
  readonly resumo = signal<ResumoFinanceiro | null>(null);
  readonly editandoSalario = signal(false);
  readonly loading = signal(true);

  readonly percentualClass = computed(() => {
    const p = this.resumo()?.percentualDoSalario ?? 0;
    if (p >= 30) return 'danger';
    if (p >= 15) return 'warning';
    return 'success';
  });

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

  private readonly assinaturaService = inject(AssinaturaService);
  private readonly usuarioService = inject(UsuarioService);
  private readonly relatorioService = inject(RelatorioService);
  private readonly fb = inject(FormBuilder);

  readonly salarioForm = this.fb.group({
    salario: [null as number | null, [Validators.required, Validators.min(0.01)]]
  });

  ngOnInit(): void {
    this.carregarDados();
    this.carregarResumo();
  }

  carregarDados(): void {
    this.loading.set(true);
    this.usuarioService.buscarPerfil().subscribe(u => {
      this.usuario.set(u);
      this.salarioForm.patchValue({ salario: u.salario });
    });
  }

  carregarResumo(): void {
    this.assinaturaService.resumoFinanceiro().subscribe(r => {
      this.resumo.set(r);
      this.loading.set(false);
    });
  }

  salvarSalario(): void {
    if (this.salarioForm.invalid) return;
    this.usuarioService.atualizarSalario(this.salarioForm.getRawValue() as AtualizarSalario).subscribe(u => {
      this.usuario.set(u);
      this.editandoSalario.set(false);
      this.carregarResumo();
    });
  }

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
