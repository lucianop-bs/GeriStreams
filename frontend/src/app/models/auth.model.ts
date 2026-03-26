/**
 * auth.model.ts — Modelos/Interfaces de Dados para Autenticação
 *
 * Este arquivo define o CONTRATO (formato esperado) dos dados que são
 * enviados e recebidos das APIs de autenticação.
 *
 * Interfaces são um recurso do TypeScript que define a "forma" de um objeto.
 * Elas garantem type safety: se você tentar usar login.nome quando deveria
 * usar login.email, o TypeScript vai reclamar ANTES de você rodar o código.
 *
 * Nota: Interfaces são apenas para desenvolvimento (TypeScript).
 * Em produção, elas viram apenas comentários - o JavaScript final não
 * contém informações de tipo.
 */

/**
 * LoginRequest — Dados que o cliente ENVIA para o servidor ao fazer login
 *
 * Campo       | Tipo    | Descrição
 * ------------|---------|-------------------------------------------
 * email       | string  | Email do usuário (ex: "joao@email.com")
 * senha       | string  | Senha em texto simples (será hasheada no servidor)
 *
 * Uso no LoginComponent:
 * const loginData: LoginRequest = {
 *   email: 'joao@email.com',
 *   senha: '123456'
 * };
 * this.authService.login(loginData).subscribe(...);
 */
export interface LoginRequest {
  email: string;
  senha: string;
}

/**
 * RegisterRequest — Dados que o cliente ENVIA para registrar novo usuário
 *
 * Campo       | Tipo    | Descrição
 * ------------|---------|-------------------------------------------
 * nome        | string  | Nome completo do usuário (ex: "João Silva")
 * email       | string  | Email único (não pode ter duplicado)
 * senha       | string  | Senha (mínimo 6 caracteres)
 * salario     | number  | Salário mensal em reais (ex: 3000.00)
 *
 * Uso no RegisterComponent:
 * const registerData: RegisterRequest = {
 *   nome: 'João Silva',
 *   email: 'joao@email.com',
 *   senha: '123456',
 *   salario: 3000
 * };
 * this.authService.register(registerData).subscribe(...);
 */
export interface RegisterRequest {
  nome: string;
  email: string;
  senha: string;
  salario: number;
}

/**
 * JwtResponse — Dados que o servidor RETORNA após login/registro bem-sucedido
 *
 * Campo       | Tipo    | Descrição
 * ------------|---------|-------------------------------------------
 * token       | string  | JWT Token (ex: "eyJhbGciOi...")
 *             |         | - Usado para autenticar próximas requisições
 *             |         | - Armazenado em localStorage
 * tipo        | string  | Tipo de autenticação (ex: "Bearer")
 *             |         | - Indica como usar o token
 *             |         | - Sempre "Bearer" no padrão OAuth 2.0
 * email       | string  | Email do usuário autenticado
 *             |         | - Confirmação de quem foi autenticado
 * role        | string  | Papel/permissão do usuário (ex: "USER" ou "ADMIN")
 *             |         | - USER: acesso normal (dashboard, assinaturas)
 *             |         | - ADMIN: acesso a painel administrativo
 *
 * Fluxo de recebimento:
 * 1. Frontend envia LoginRequest
 * 2. Servidor valida email/senha
 * 3. Servidor retorna JwtResponse (incluindo token JWT)
 * 4. Frontend armazena token em localStorage
 * 5. Próximas requisições usam este token no header Authorization
 *
 * Exemplo de resposta do servidor:
 * {
 *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "tipo": "Bearer",
 *   "email": "joao@email.com",
 *   "role": "USER"
 * }
 */
export interface JwtResponse {
  token: string;
  tipo: string;
  email: string;
  role: string;
}
