import {Component, inject, OnInit, signal} from '@angular/core';
import {CurrencyPipe} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {AssinaturaService} from '../../services/assinatura.service';
import {AssinaturaRequest, AssinaturaResponse, CATEGORIAS, PLATAFORMAS_PREDEFINIDAS, PlataformaOpcao} from '../../models/assinatura.model';
import {NavbarComponent} from '../navbar/navbar.component';

@Component({
  selector: 'app-subscriptions',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe, NavbarComponent],
  templateUrl: './subscriptions.component.html'
})
export class SubscriptionsComponent implements OnInit {

  readonly assinaturas = signal<AssinaturaResponse[]>([]);
  readonly categorias = CATEGORIAS;
  readonly plataformas: PlataformaOpcao[] = PLATAFORMAS_PREDEFINIDAS;
  readonly editandoId = signal<number | null>(null);
  readonly mostrarFormulario = signal(false);
  readonly loading = signal(false);
  readonly usandoOutro = signal(false);
  readonly plataformaSelect = signal<string>('');
  private readonly assinaturaService = inject(AssinaturaService);
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    valor: [null as number | null, [Validators.required, Validators.min(0.01)]],
    categoria: ['', Validators.required]
  });

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.assinaturaService.listar().subscribe(a => this.assinaturas.set(a));
  }

  abrirFormulario(assinatura?: AssinaturaResponse): void {
    this.mostrarFormulario.set(true);
    if (assinatura) {
      this.editandoId.set(assinatura.id);
      this.form.patchValue(assinatura);
      const plataformaConhecida = this.plataformas.find(p => p.nome === assinatura.nome);
      if (plataformaConhecida) {
        this.plataformaSelect.set(assinatura.nome);
        this.usandoOutro.set(false);
      } else {
        this.plataformaSelect.set('__outro__');
        this.usandoOutro.set(true);
      }
    } else {
      this.editandoId.set(null);
      this.form.reset();
      this.plataformaSelect.set('');
      this.usandoOutro.set(false);
    }
  }

  fecharFormulario(): void {
    this.mostrarFormulario.set(false);
    this.editandoId.set(null);
    this.form.reset();
    this.plataformaSelect.set('');
    this.usandoOutro.set(false);
  }

  onPlataformaSelecionada(nome: string): void {
    this.plataformaSelect.set(nome);
    if (nome === '__outro__') {
      this.usandoOutro.set(true);
      this.form.get('nome')?.setValue('');
    } else {
      this.usandoOutro.set(false);
      this.form.get('nome')?.setValue(nome);
      const plataforma = this.plataformas.find(p => p.nome === nome);
      if (plataforma) {
        this.form.get('categoria')?.setValue(plataforma.categoria);
      }
    }
  }

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

  remover(id: number): void {
    if (!confirm('Deseja remover esta assinatura?')) return;
    this.assinaturaService.remover(id).subscribe(() => this.carregar());
  }

  toggleAtivo(id: number): void {
    this.assinaturaService.toggleAtivo(id).subscribe(() => this.carregar());
  }

  labelCategoria(value: string): string {
    return this.categorias.find(c => c.value === value)?.label ?? value;
  }
}
