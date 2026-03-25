import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AtualizarSalario, Usuario } from '../models/usuario.model';
import { AssinaturaResponse } from '../models/assinatura.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class UsuarioService {

  private readonly apiUrl = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

  buscarPerfil(): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.apiUrl}/users/me`);
  }

  atualizarSalario(payload: AtualizarSalario): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.apiUrl}/users/me/salario`, payload);
  }

  // Admin
  listarTodos(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.apiUrl}/admin/users`);
  }

  buscarPorId(id: number): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.apiUrl}/admin/users/${id}`);
  }

  listarAssinaturasDoUsuario(id: number): Observable<AssinaturaResponse[]> {
    return this.http.get<AssinaturaResponse[]>(`${this.apiUrl}/admin/users/${id}/subscriptions`);
  }

  // UC15
  promoverParaAdmin(id: number): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.apiUrl}/admin/users/${id}/promote`, {});
  }
}
