import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import { CommonModule, CurrencyPipe, DecimalPipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from '../navbar/navbar.component';
import { AiTipsComponent } from '../ai-tips/ai-tips.component';
import { AssinaturaService } from '../../services/assinatura.service';
import { UsuarioService } from '../../services/usuario.service';
import { RelatorioService } from '../../services/relatorio.service';
import { ResumoFinanceiro } from '../../models/financeiro.model';
import { Usuario } from '../../models/usuario.model';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent, AiTipsComponent, ReactiveFormsModule, CurrencyPipe, DecimalPipe],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {

  usuario: Usuario | null = null;
  resumo: ResumoFinanceiro | null = null;
  salarioForm: FormGroup;
  editandoSalario = false;
  loading = true;

  constructor(
    private assinaturaService: AssinaturaService,
    private usuarioService: UsuarioService,
    private relatorioService: RelatorioService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef

  ) {
    this.salarioForm = this.fb.group({
      salario: [null, [Validators.required, Validators.min(0.01)]]
    });
  }

  ngOnInit(): void {
    this.carregarDados();
    this.carregarTipo()
  }

  carregarDados(): void {
    this.loading = true;
    this.usuarioService.buscarPerfil().subscribe(u => {
      this.usuario = u;
      this.salarioForm.patchValue({ salario: u.salario });
    });
  }
  carregarTipo():void{
    this.assinaturaService.resumoFinanceiro().subscribe(r => {
      this.resumo = r;
      this.loading = false;
      this.cdr.detectChanges();})
  }

  salvarSalario(): void {
    if (this.salarioForm.invalid) return;
    this.usuarioService.atualizarSalario(this.salarioForm.value).subscribe(u => {
      this.usuario = u;
      this.editandoSalario = false;
      this.carregarDados();
    });
  }

  get percentualClass(): string {
    const p = this.resumo?.percentualDoSalario ?? 0;
    if (p >= 30) return 'danger';
    if (p >= 15) return 'warning';
    return 'success';
  }

  // UC17
  baixarRelatorio(): void {
    this.relatorioService.exportarPdf().subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `relatorio-geristreams-${new Date().toISOString().slice(0,10)}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  get categoriasEntries(): { nome: string; valor: number }[] {
    if (!this.resumo) return [];
    return Object.entries(this.resumo.gastosPorCategoria)
      .map(([nome, valor]) => ({ nome, valor }))
      .sort((a, b) => b.valor - a.valor);
  }

  protected readonly console = console;
}
