import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NavbarComponent } from '../navbar/navbar.component';
import { AssinaturaService } from '../../services/assinatura.service';
import { AssinaturaResponse } from '../../models/assinatura.model';
import { CATEGORIAS } from '../../models/assinatura.model';

@Component({
  selector: 'app-subscriptions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent, CurrencyPipe],
  templateUrl: './subscriptions.component.html'
})
export class SubscriptionsComponent implements OnInit {

  assinaturas: AssinaturaResponse[] = [];
  categorias = CATEGORIAS;
  form: FormGroup;
  editandoId: number | null = null;
  mostrarFormulario = false;
  loading = false;

  constructor(private assinaturaService: AssinaturaService, private fb: FormBuilder,private cdr: ChangeDetectorRef) {
    this.form = this.fb.group({
      nome:      ['', [Validators.required, Validators.maxLength(100)]],
      valor:     [null, [Validators.required, Validators.min(0.01)]],
      categoria: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.assinaturaService.listar().subscribe(a => {
      this.assinaturas = a;
      this.cdr.detectChanges();
    })
  }

  abrirFormulario(assinatura?: AssinaturaResponse): void {
    this.mostrarFormulario = true;
    if (assinatura) {
      this.editandoId = assinatura.id;
      this.form.patchValue(assinatura);
    } else {
      this.editandoId = null;
      this.form.reset();
    }
  }

  fecharFormulario(): void {
    this.mostrarFormulario = false;
    this.editandoId = null;
    this.form.reset();
  }

  salvar(): void {
    if (this.form.invalid) return;
    this.loading = true;
    const payload = this.form.value;

    const request$ = this.editandoId
      ? this.assinaturaService.atualizar(this.editandoId, payload)
      : this.assinaturaService.criar(payload);

    request$.subscribe({
      next: () => {
        this.fecharFormulario();
        this.carregar();
        this.loading = false;
      },
      error: () => (this.loading = false)
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
