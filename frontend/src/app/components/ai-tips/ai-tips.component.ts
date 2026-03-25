import {ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AiService } from '../../services/ai.service';

@Component({
  selector: 'app-ai-tips',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ai-tips.component.html'
})
export class AiTipsComponent {

  dicas: string | null = null;
  loading = false;
  erro: string | null = null;
  gerado = false;

  constructor(private aiService: AiService,private cdr: ChangeDetectorRef) {}

  gerarDicas(): void {
    this.loading = true;
    this.dicas = null;
    this.erro = null;

    this.aiService.gerarDicas().subscribe({
      next: res => {
        this.dicas = res.dicas;
        this.loading = false;
        this.gerado = true;
        this.cdr.detectChanges()
      },
      error: err => {
        this.erro = err.error?.error ?? 'Não foi possível gerar as dicas. Tente novamente.';
        this.loading = false;
      }
    });
  }

  /**
   * Converte o markdown simples retornado pelo Claude em HTML seguro.
   * Suporta: **negrito**, listas numeradas e quebras de linha.
   */
  get dicasHtml(): string {
    if (!this.dicas) return '';

    return this.dicas
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/^(\d+)\.\s(.+)$/gm, '<li>$2</li>')
      .replace(/(<li>.*<\/li>(\n|$))+/g, match => `<ol>${match}</ol>`)
      .replace(/^- (.+)$/gm, '<li>$1</li>')
      .replace(/(<li>.*<\/li>(\n|$))+/g, match => `<ul>${match}</ul>`)
      .replace(/\n/g, '<br>');
  }
}
