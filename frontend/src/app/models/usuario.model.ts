/**
 * usuario.model.ts — Modelos de Dados de Usuário
 *
 * Este arquivo define as interfaces que representam um usuário e
 * os dados que podem ser atualizados.
 */

/**
 * Usuario — Dados completos de um usuário no sistema
 *
 * Campo       | Tipo              | Descrição
 * ------------|-------------------|-------------------------------------------
 * id          | number            | ID único do usuário no banco de dados
 *             |                   | - Gerado automaticamente pelo servidor
 *             |                   | - Usado para identificar usuário em requisições
 * nome        | string            | Nome completo do usuário
 *             |                   | - Ex: "João Silva"
 * email       | string            | Email único do usuário
 *             |                   | - Chave para fazer login
 *             |                   | - Não pode haver 2 usuários com mesmo email
 * salario     | number            | Salário mensal em reais
 *             |                   | - Ex: 3000.00
 *             |                   | - Usado para calcular percentual de gastos
 * role        | 'USER' | 'ADMIN'  | Papel do usuário (tipo enumerado)
 *             |                   | - USER: usuário normal, acesso básico
 *             |                   | - ADMIN: administrador, acesso ao painel admin
 * createdAt   | string            | Data/hora de criação da conta
 *             |                   | - Formato ISO (ex: "2026-03-25T10:30:00Z")
 *             |                   | - Gerado automaticamente pelo servidor
 *
 * Uso:
 * const usuario: Usuario = {
 *   id: 1,
 *   nome: 'João Silva',
 *   email: 'joao@email.com',
 *   salario: 3000,
 *   role: 'USER',
 *   createdAt: '2026-03-25T10:30:00Z'
 * };
 */
export interface Usuario {
  id: number;
  nome: string;
  email: string;
  salario: number;
  role: 'USER' | 'ADMIN';           // Type union: pode ser APENAS 'USER' ou 'ADMIN'
  createdAt: string;
}

/**
 * AtualizarSalario — Dados para atualizar o salário de um usuário
 *
 * Campo       | Tipo    | Descrição
 * ------------|---------|-------------------------------------------
 * salario     | number  | Novo salário mensal em reais
 *
 * Por que uma interface separada?
 * - Para atualizar salário, o cliente envia APENAS o novo valor
 * - Não faz sentido enviar todos os dados do usuário novamente
 * - Esta interface deixa claro qual é o contrato desta operação específica
 *
 * Uso no DashboardComponent:
 * const novoSalario: AtualizarSalario = { salario: 3500 };
 * this.usuarioService.atualizarSalario(novoSalario).subscribe(...);
 */
export interface AtualizarSalario {
  salario: number;
}
