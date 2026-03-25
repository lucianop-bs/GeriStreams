export interface Usuario {
  id: number;
  nome: string;
  email: string;
  salario: number;
  role: 'USER' | 'ADMIN';
  createdAt: string;
}

export interface AtualizarSalario {
  salario: number;
}
