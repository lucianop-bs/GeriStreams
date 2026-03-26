import {Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Router} from '@angular/router';
import {Observable, tap} from 'rxjs';
import {JwtResponse, LoginRequest, RegisterRequest} from '../models/auth.model';
import {environment} from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly apiUrl = `${environment.apiUrl}/api/auth`;
  private readonly TOKEN_KEY = 'geristreams_token';
  private readonly ROLE_KEY = 'geristreams_role';

  private readonly _loggedIn = signal(this.hasToken());
  readonly loggedIn = this._loggedIn.asReadonly();

  constructor(private http: HttpClient, private router: Router) {}

  login(payload: LoginRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.apiUrl}/login`, payload).pipe(
      tap(res => this.storeSession(res))
    );
  }

  register(payload: RegisterRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.apiUrl}/register`, payload).pipe(
      tap(res => this.storeSession(res))
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.ROLE_KEY);
    this._loggedIn.set(false);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getRole(): string | null {
    return localStorage.getItem(this.ROLE_KEY);
  }

  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  private hasToken(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }

  private storeSession(res: JwtResponse): void {
    localStorage.setItem(this.TOKEN_KEY, res.token);
    localStorage.setItem(this.ROLE_KEY, res.role);
    this._loggedIn.set(true);
  }
}
