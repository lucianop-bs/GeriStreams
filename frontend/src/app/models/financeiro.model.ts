import { AssinaturaResponse } from './assinatura.model';

export interface ResumoFinanceiro {
  salario: number;
  totalMensal: number;
  percentualDoSalario: number;
  assinaturas: AssinaturaResponse[];
  gastosPorCategoria: Record<string, number>;
}
