import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AssinaturaRequest, AssinaturaResponse } from '../models/assinatura.model';
import { ResumoFinanceiro } from '../models/financeiro.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AssinaturaService {

  private readonly apiUrl = `${environment.apiUrl}/api/subscriptions`;

  constructor(private http: HttpClient) {}

  listar(): Observable<AssinaturaResponse[]> {
    return this.http.get<AssinaturaResponse[]>(this.apiUrl);
  }

  criar(payload: AssinaturaRequest): Observable<AssinaturaResponse> {
    return this.http.post<AssinaturaResponse>(this.apiUrl, payload);
  }

  atualizar(id: number, payload: AssinaturaRequest): Observable<AssinaturaResponse> {
    return this.http.put<AssinaturaResponse>(`${this.apiUrl}/${id}`, payload);
  }

  remover(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  toggleAtivo(id: number): Observable<AssinaturaResponse> {
    return this.http.patch<AssinaturaResponse>(`${this.apiUrl}/${id}/toggle`, {});
  }

  resumoFinanceiro(): Observable<ResumoFinanceiro> {
    return this.http.get<ResumoFinanceiro>(`${this.apiUrl}/resumo`);
  }
}
