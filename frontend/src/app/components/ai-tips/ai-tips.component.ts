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

  readonly dicas = signal<string | null>(null);
  readonly loading = signal(false);
  readonly erro = signal<string | null>(null);
  readonly gerado = signal(false);

  readonly dicasHtml = computed(() => {
    const d = this.dicas();
    if (!d) return '';
    return marked.parse(d, {async: false}) as string;
  });

  private readonly aiService = inject(AiService);

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
