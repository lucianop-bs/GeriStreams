export interface LoginRequest {
  email: string;
  senha: string;
}

export interface RegisterRequest {
  nome: string;
  email: string;
  senha: string;
  salario: number;
}

export interface JwtResponse {
  token: string;
  tipo: string;
  email: string;
  role: string;
}
