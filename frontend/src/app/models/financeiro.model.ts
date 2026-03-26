/**
 * financeiro.model.ts — Modelos para dados financeiros do usuário
 *
 * Define a estrutura de dados que representa o resumo financeiro de um usuário,
 * incluindo análise de gastos, categorias e percentuais.
 */

import {AssinaturaResponse} from './assinatura.model';

/**
 * ResumoFinanceiro — Análise completa dos gastos financeiros do usuário
 *
 * Campo                | Tipo                           | Descrição
 * ---------------------|--------------------------------|-------------------------------------------
 * salario              | number                         | Salário mensal do usuário (ex: 3000)
 * totalMensal          | number                         | Total gasto em assinaturas ativas/mês
 *                      |                                | - Soma de todos os valores ativos
 *                      |                                | - Ex: 49.90 + 15.00 + 50 = 114.90
 * percentualDoSalario  | number                         | % do salário gasto em assinaturas
 *                      |                                | - Cálculo: (totalMensal / salario) * 100
 *                      |                                | - Ex: (114.90 / 3000) * 100 = 3.83%
 * assinaturas          | AssinaturaResponse[]           | Array com TODAS as assinaturas do usuário
 *                      |                                | - Inclui ativas E inativas
 *                      |                                | - Cada item tem: id, nome, valor, categoria, ativo
 * gastosPorCategoria   | Record<string, number>         | Objeto mapeando categoria → total gasto
 *                      |                                | - Record é um tipo TypeScript para "dicionário"
 *                      |                                | - Exemplo:
 *                      |                                |   {
 *                      |                                |     "STREAMING_VIDEO": 49.90,
 *                      |                                |     "STREAMING_MUSICA": 15.00,
 *                      |                                |     "JOGOS": 50.00
 *                      |                                |   }
 *
 * Uso principal:
 * - Exibir cards no DashboardComponent com resumo financeiro
 * - Calcular cores (verde/amarelo/vermelho) baseado em percentualDoSalario
 * - Montar gráfico de gastos por categoria
 * - Listar todas as assinaturas do usuário
 *
 * Exemplo de resposta do servidor:
 * {
 *   "salario": 3000,
 *   "totalMensal": 114.90,
 *   "percentualDoSalario": 3.83,
 *   "assinaturas": [
 *     {
 *       "id": 1,
 *       "nome": "Netflix",
 *       "valor": 49.90,
 *       "categoria": "STREAMING_VIDEO",
 *       "ativo": true,
 *       "createdAt": "2026-03-25T10:30:00Z"
 *     },
 *     ...
 *   ],
 *   "gastosPorCategoria": {
 *     "STREAMING_VIDEO": 49.90,
 *     "STREAMING_MUSICA": 15.00,
 *     "JOGOS": 50.00
 *   }
 * }
 */
export interface ResumoFinanceiro {
  salario: number;
  totalMensal: number;
  percentualDoSalario: number;
  assinaturas: AssinaturaResponse[];
  gastosPorCategoria: Record<string, number>;  // Record<K, V> = { [key: K]: V }
}
