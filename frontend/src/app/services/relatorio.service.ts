/**
 * relatorio.service.ts — Serviço de Relatórios e Análises
 *
 * Responsabilidade: Comunicar com endpoints de geração de relatórios.
 *
 * Operações:
 * - Exportar relatório em PDF [UC17]
 * - Ranking de serviços mais usados [UC16]
 */

import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';

/**
 * RankingAssinatura — Dados de um serviço no ranking
 *
 * Campo               | Tipo    | Descrição
 * --------------------|---------|-------------------------------------------
 * nomeServico         | string  | Nome do serviço (ex: "Netflix", "Spotify")
 * totalAssinantes     | number  | Quantos usuários têm este serviço
 *                     |         | - Ex: 150 usuários têm Netflix
 * valorTotalMensal    | number  | Quanto a plataforma ganha com este serviço
 *                     |         | - Ex: 150 * R$ 49.90 = R$ 7.485
 * valorMedio          | number  | Valor médio pago por assinante
 *                     |         | - Ex: R$ 49.90 (se todos pagam igual)
 *
 * Uso:
 * Mostrar ranking na página de admin com qual serviço é mais popular.
 * Exemplo:
 * 1. 🥇 Netflix - 150 assinantes - R$ 7.485/mês - Média R$ 49.90
 * 2. 🥈 Spotify - 120 assinantes - R$ 1.800/mês - Média R$ 15.00
 * 3. 🥉 Disney+ - 80 assinantes - R$ 720/mês - Média R$ 9.00
 */
export interface RankingAssinatura {
  nomeServico: string;
  totalAssinantes: number;
  valorTotalMensal: number;
  valorMedio: number;
}

@Injectable({ providedIn: 'root' })
export class RelatorioService {

  private readonly apiUrl = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

  /**
   * exportarPdf() — GET /api/reports/pdf [UC17]
   *
   * Propósito: Baixar relatório em PDF com dados financeiros do usuário
   *
   * Fluxo:
   * 1. Usuario clica em "Exportar PDF" no DashboardComponent
   * 2. Chama exportarPdf()
   * 3. Envia GET /api/reports/pdf
   * 4. Servidor gera PDF em memória (usando biblioteca como iText, etc)
   * 5. Servidor retorna PDF como arquivo binário (Blob)
   * 6. Frontend recebe Blob
   * 7. Cria URL temporária para o Blob
   * 8. Simula clique em <a> dinamicamente
   * 9. Browser faz download do arquivo
   *
   * Blob (Binary Large Object):
   * - Tipo especial de dado em JavaScript
   * - Representa arquivo binário (PDF, imagem, vídeo, etc)
   * - Não é texto, é arquivo bruto em bytes
   *
   * ResponseType 'blob':
   * - Normalmente, HttpClient trata respostas como JSON
   * - responseType: 'blob' diz: "não tente parsear como JSON"
   * - "Me retorne o arquivo binário bruto"
   * - Sem isso, tentaria fazer JSON.parse(pdfBytes) e falharia
   *
   * @returns Observable<Blob> com arquivo PDF
   *
   * Uso no DashboardComponent:
   * ```typescript
   * baixarRelatorio(): void {
   *   this.relatorioService.exportarPdf().subscribe(blob => {
   *     const url = URL.createObjectURL(blob);
   *     const a = document.createElement('a');
   *     a.href = url;
   *     a.download = 'relatorio-geristreams-2026-03-25.pdf';
   *     a.click();
   *     URL.revokeObjectURL(url);
   *   });
   * }
   * ```
   *
   * Conteúdo do PDF:
   * - Nome do usuário
   * - Período do relatório
   * - Lista de assinaturas ativas
   * - Total mensal
   * - Percentual do salário
   * - Gráfico de gastos por categoria (se gerador PDF suporta)
   */
  exportarPdf(): Observable<Blob> {
    // responseType: 'blob' crucial aqui!
    // Diz ao Angular: "retorne arquivo binário, não tente fazer JSON.parse()"
    return this.http.get(`${this.apiUrl}/reports/pdf`, { responseType: 'blob' });
  }

  /**
   * rankingServicos() — GET /api/admin/ranking [UC16]
   *
   * Propósito: Obter ranking dos serviços mais usados na plataforma (apenas ADMIN)
   *
   * Acesso: Apenas usuários com role = 'ADMIN'
   * AdminGuard na rota protege este acesso
   *
   * Fluxo:
   * 1. Admin abre página /admin
   * 2. AdminComponent ngOnInit() chama rankingServicos()
   * 3. Envia GET /api/admin/ranking
   * 4. Servidor:
   *    - Conta quantos usuários têm cada serviço
   *    - Soma quanto ganhou com cada serviço
   *    - Calcula valor médio por assinante
   *    - Ordena decrescente por total (maior primeiro)
   * 5. Retorna array de RankingAssinatura
   * 6. Frontend mostra tabela com medalhas (🥇, 🥈, 🥉)
   *
   * Dados retornados:
   * [
   *   {
   *     "nomeServico": "Netflix",
   *     "totalAssinantes": 150,
   *     "valorTotalMensal": 7485.00,
   *     "valorMedio": 49.90
   *   },
   *   {
   *     "nomeServico": "Spotify",
   *     "totalAssinantes": 120,
   *     "valorTotalMensal": 1800.00,
   *     "valorMedio": 15.00
   *   },
   *   ...
   * ]
   *
   * @returns Observable<RankingAssinatura[]> com ranking ordenado
   *
   * Uso no AdminComponent:
   * ```typescript
   * this.relatorioService.rankingServicos().subscribe(r => {
   *   this.ranking = r; // Mostra em tabela
   * });
   * ```
   *
   * Casos de uso:
   * - Admin analisa quais serviços são mais populares
   * - Admin identifica tendências (qual tipo de assinatura cresce)
   * - Admin toma decisões estratégicas baseado em dados
   * - Relatórios executivos para stakeholders
   */
  rankingServicos(): Observable<RankingAssinatura[]> {
    return this.http.get<RankingAssinatura[]>(`${this.apiUrl}/admin/ranking`);
  }
}
