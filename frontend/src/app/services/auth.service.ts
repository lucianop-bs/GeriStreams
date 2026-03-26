import {Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Router} from '@angular/router';
import {Observable, tap} from 'rxjs';
import {JwtResponse, LoginRequest, RegisterRequest} from '../models/auth.model';
import {environment} from '../../environments/environment';

/**
 * SERVICE: AuthService
 *
 * Responsabilidade: Gerenciar autenticação do usuário (login, registro, logout)
 * e armazenar/recuperar JWT token.
 *
 * Gestão de Estado Reativo com Signals:
 * - _loggedIn: WritableSignal interno que mantém o estado de autenticação
 * - loggedIn: Signal público (somente leitura) exposto para componentes
 * - Signals substituem BehaviorSubject para reatividade síncrona e performática
 * - Componentes acessam loggedIn() no template → atualização automática da view
 *
 * Fluxo de Autenticação:
 * 1. Usuário faz login com email/senha
 * 2. Backend retorna JWT token + role (USER ou ADMIN)
 * 3. Token é armazenado em localStorage
 * 4. Signal _loggedIn é atualizado para true
 * 5. Interceptador JWT adiciona token em todas as requisições
 * 6. Ao logout, token é removido e signal emite false
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly apiUrl = `${environment.apiUrl}/api/auth`;
  private readonly TOKEN_KEY = 'geristreams_token';
  private readonly ROLE_KEY = 'geristreams_role';

  /**
   * _loggedIn: WritableSignal<boolean> — estado interno de autenticação
   *
   * Inicializado com hasToken() para refletir o estado real ao carregar a página.
   * Apenas métodos internos podem alterar (.set()).
   */
  private readonly _loggedIn = signal(this.hasToken());

  /**
   * loggedIn: Signal<boolean> — estado público de autenticação (somente leitura)
   *
   * Exposto como asReadonly() para que componentes possam ler mas não alterar.
   * Uso no template: @if (authService.loggedIn()) { ... }
   * Uso em computed(): computed(() => this.authService.loggedIn() && ...)
   */
  readonly loggedIn = this._loggedIn.asReadonly();

  constructor(private http: HttpClient, private router: Router) {}

  /**
   * login() — Autentica usuário com email e senha
   *
   * @param payload - { email: string, senha: string }
   * @returns Observable<JwtResponse> com token JWT e role
   *
   * Fluxo:
   * 1. POST /api/auth/login com credenciais
   * 2. pipe(tap) intercepta resposta para armazenar sessão
   * 3. storeSession() salva token + atualiza signal
   */
  login(payload: LoginRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.apiUrl}/login`, payload).pipe(
      tap(res => this.storeSession(res))
    );
  }

  /**
   * register() — Registra novo usuário e já autentica
   *
   * @param payload - { nome, email, senha, salario }
   * @returns Observable<JwtResponse> com token JWT (já logado após registro)
   */
  register(payload: RegisterRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.apiUrl}/register`, payload).pipe(
      tap(res => this.storeSession(res))
    );
  }

  /**
   * logout() — Remove sessão e redireciona para login
   *
   * Fluxo:
   * 1. Remove token e role do localStorage
   * 2. Atualiza signal _loggedIn para false (notifica componentes)
   * 3. Navega para /login
   */
  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.ROLE_KEY);
    this._loggedIn.set(false);
    this.router.navigate(['/login']);
  }

  /** Recupera JWT token do localStorage (ou null se ausente) */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /** Recupera role do usuário ('USER' | 'ADMIN' | null) */
  getRole(): string | null {
    return localStorage.getItem(this.ROLE_KEY);
  }

  /** Verifica se o usuário atual é administrador */
  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  /** Verifica se token existe no localStorage */
  private hasToken(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * storeSession() — Armazena token/role e atualiza estado reativo
   *
   * @param res - Resposta da API { token, role }
   *
   * localStorage persiste os dados entre sessões do navegador.
   * O signal _loggedIn.set(true) notifica todos os computed() e templates
   * que dependem de loggedIn(), atualizando a view automaticamente.
   */
  private storeSession(res: JwtResponse): void {
    localStorage.setItem(this.TOKEN_KEY, res.token);
    localStorage.setItem(this.ROLE_KEY, res.role);
    this._loggedIn.set(true);
  }
}
