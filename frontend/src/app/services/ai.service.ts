import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AiDicasResponse {
  dicas: string;
}

@Injectable({ providedIn: 'root' })
export class AiService {

  private readonly apiUrl = `${environment.apiUrl}/api/ai`;

  constructor(private http: HttpClient) {}

  gerarDicas(): Observable<AiDicasResponse> {
    return this.http.get<AiDicasResponse>(`${this.apiUrl}/dicas`);
  }
}
