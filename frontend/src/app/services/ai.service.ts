/**
 * ai.service.ts — Serviço de Integração com IA (Claude)
 *
 * Responsabilidade: Comunicar com a API de IA para gerar dicas personalizadas.
 *
 * Integração:
 * - Backend faz requisição para Claude AI (Anthropic)
 * - Passa dados financeiros do usuário
 * - IA analisa e retorna dicas em Markdown
 * - Frontend renderiza Markdown como HTML
 */

import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';

/**
 * AiDicasResponse — Resposta da API de IA
 *
 * Campo  | Tipo   | Descrição
 * -------|--------|-------------------------------------------
 * dicas  | string | Texto em Markdown com análise e dicas
 *        |        | - Contém títulos, listas, formatação
 *        |        | - Será convertido para HTML no frontend
 *
 * Exemplo de retorno:
 * {
 *   "dicas": "# Análise de Assinaturas\n\nVocê está gastando 15% do seu salário...\n\n## Recomendações\n- Cancele..."
 * }
 */
export interface AiDicasResponse {
  dicas: string;
}

@Injectable({ providedIn: 'root' })
export class AiService {

  // URL base da API de IA
  // Exemplo: 'http://localhost:8080/api/ai'
  private readonly apiUrl = `${environment.apiUrl}/api/ai`;

  constructor(private http: HttpClient) {}

  /**
   * gerarDicas() — GET /api/ai/dicas
   *
   * Propósito: Gerar dicas personalizadas usando Claude AI
   *
   * Fluxo:
   * 1. Frontend chama gerarDicas()
   * 2. Backend recebe requisição
   * 3. Backend busca dados do usuário (salário, assinaturas)
   * 4. Backend monta prompt para Claude
   * 5. Claude analisa e retorna texto em Markdown
   * 6. Backend retorna resposta ao frontend
   * 7. Frontend converte Markdown para HTML usando 'marked'
   * 8. Template exibe com [innerHTML]
   *
   * Exemplo de prompt enviado para Claude:
   * "Analise o perfil financeiro de João:
   *  - Salário: R$ 3000
   *  - Assinaturas ativas: Netflix (R$ 49.90), Spotify (R$ 15), etc
   *  - Total mensal: R$ 114.90 (3.8% do salário)
   *
   *  Gere dicas personalizadas em Markdown."
   *
   * Claude retorna algo como:
   * "# Análise de Assinaturas
   *  Você está gastando apenas 3.8% do seu salário em assinaturas,
   *  o que é **excelente**! Aqui estão algumas recomendações..."
   *
   * @returns Observable<AiDicasResponse> com propriedade dicas (Markdown)
   *
   * Uso no AiTipsComponent:
   * ```typescript
   * this.aiService.gerarDicas().subscribe({
   *   next: res => {
   *     this.dicas = res.dicas; // Armazena Markdown
   *     // dicasHtml getter converte para HTML
   *   },
   *   error: err => this.erro = err.error?.error
   * });
   * ```
   *
   * Segurança:
   * - Apenas usuários autenticados podem chamar
   * - JwtInterceptor adiciona token automaticamente
   * - Servidor valida permissão antes de processar
   */
  gerarDicas(): Observable<AiDicasResponse> {
    return this.http.get<AiDicasResponse>(`${this.apiUrl}/dicas`);
  }
}
