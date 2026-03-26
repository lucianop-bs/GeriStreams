import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';

export interface RankingAssinatura {
  nomeServico: string;
  totalAssinantes: number;
  valorTotalMensal: number;
  valorMedio: number;
}

@Injectable({ providedIn: 'root' })
export class RelatorioService {

  private readonly apiUrl = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

  exportarPdf(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/reports/pdf`, { responseType: 'blob' });
  }

  rankingServicos(): Observable<RankingAssinatura[]> {
    return this.http.get<RankingAssinatura[]>(`${this.apiUrl}/admin/ranking`);
  }
}
