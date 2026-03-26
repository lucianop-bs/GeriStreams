/**
 * assinatura.service.ts — Serviço de Requisições de Assinaturas
 *
 * Responsabilidade: Fazer comunicação HTTP com endpoints de assinaturas.
 *
 * CRUD Completo:
 * - Create (POST): criar nova assinatura
 * - Read (GET): buscar assinaturas
 * - Update (PUT): atualizar assinatura
 * - Delete (DELETE): remover assinatura
 *
 * Operações adicionais:
 * - Toggle: ativar/desativar assinatura
 * - Resumo Financeiro: análise de gastos
 */

import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {AssinaturaRequest, AssinaturaResponse} from '../models/assinatura.model';
import {ResumoFinanceiro} from '../models/financeiro.model';
import {environment} from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AssinaturaService {

  // URL base para todas as operações de assinaturas
  // Exemplo: 'http://localhost:8080/api/subscriptions'
  private readonly apiUrl = `${environment.apiUrl}/api/subscriptions`;

  constructor(private http: HttpClient) {}

  // ================= LEITURA (READ) =================

  /**
   * listar() — GET /api/subscriptions
   *
   * Propósito: Buscar TODAS as assinaturas do usuário logado
   *
   * Endpoint: GET /api/subscriptions
   * - Servidor sabe qual usuário via token JWT
   * - Retorna apenas assinaturas daquele usuário
   *
   * @returns Observable<AssinaturaResponse[]> array com assinaturas
   *
   * Uso no SubscriptionsComponent:
   * ```typescript
   * this.assinaturaService.listar().subscribe(assinaturas => {
   *   this.assinaturas = assinaturas;
   * });
   * ```
   */
  listar(): Observable<AssinaturaResponse[]> {
    return this.http.get<AssinaturaResponse[]>(this.apiUrl);
  }

  // ================= CRIAÇÃO (CREATE) =================

  /**
   * criar() — POST /api/subscriptions
   *
   * Propósito: Criar uma nova assinatura para o usuário logado
   *
   * @param payload: { nome, valor, categoria }
   * @returns Observable<AssinaturaResponse> com a assinatura criada (inclui id, createdAt)
   *
   * Fluxo no SubscriptionsComponent:
   * 1. Usuário preenche formulário (Netflix, R$ 49.90, STREAMING_VIDEO)
   * 2. Clica em Salvar
   * 3. Componente envia POST /api/subscriptions com payload
   * 4. Servidor cria assinatura e retorna objeto com id e createdAt
   * 5. Componente recarrega lista
   *
   * HTTP Method POST:
   * - POST = criar novo recurso no servidor
   * - Corpo (body) contém dados do novo recurso
   * - Retorna status 201 (Created) + dados criados
   */
  criar(payload: AssinaturaRequest): Observable<AssinaturaResponse> {
    return this.http.post<AssinaturaResponse>(this.apiUrl, payload);
  }

  // ================= ATUALIZAÇÃO (UPDATE) =================

  /**
   * atualizar() — PUT /api/subscriptions/{id}
   *
   * Propósito: Atualizar uma assinatura existente
   *
   * @param id: ID da assinatura a atualizar (ex: 5)
   * @param payload: { nome, valor, categoria } — novos valores
   * @returns Observable<AssinaturaResponse> com dados atualizados
   *
   * Fluxo no SubscriptionsComponent:
   * 1. Usuário clica em editar uma assinatura
   * 2. Formulário é preenchido com dados atuais
   * 3. Usuário modifica alguns campos (ex: novo valor)
   * 4. Clica Salvar
   * 5. Componente envia PUT /api/subscriptions/5 com novo payload
   * 6. Servidor atualiza e retorna assinatura atualizada
   * 7. Lista recarrega
   *
   * HTTP Method PUT:
   * - PUT = atualizar recurso existente
   * - URL contém ID do recurso (/subscriptions/5)
   * - Corpo contém novos valores
   *
   * HTTP Method PUT vs PATCH:
   * - PUT: substitui TUDO (envie todos os campos)
   * - PATCH: atualiza PARTE (envie apenas mudanças)
   * Neste caso, PUT é correto porque AssinaturaRequest tem todos os campos
   */
  atualizar(id: number, payload: AssinaturaRequest): Observable<AssinaturaResponse> {
    return this.http.put<AssinaturaResponse>(`${this.apiUrl}/${id}`, payload);
  }

  // ================= REMOÇÃO (DELETE) =================

  /**
   * remover() — DELETE /api/subscriptions/{id}
   *
   * Propósito: Deletar uma assinatura (remover permanentemente)
   *
   * @param id: ID da assinatura a deletar (ex: 3)
   * @returns Observable<void> — DELETE geralmente não retorna dados
   *
   * Fluxo no SubscriptionsComponent:
   * 1. Usuário clica em lixo (ícone delete)
   * 2. Pede confirmação: "Tem certeza?"
   * 3. Se confirma, chama remover(id)
   * 4. Envia DELETE /api/subscriptions/3
   * 5. Servidor deleta assinatura do banco
   * 6. Retorna 204 No Content (sem corpo)
   * 7. Lista recarrega
   *
   * Observable<void>:
   * - <void> significa que a requisição não retorna dados úteis
   * - Apenas importa que foi bem-sucedida
   * - Se houver erro, subscribe() vê no callback error
   */
  remover(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ================= OPERAÇÕES ADICIONAIS =================

  /**
   * toggleAtivo() — PATCH /api/subscriptions/{id}/toggle
   *
   * Propósito: Ativar ou desativar uma assinatura (pausar sem deletar)
   *
   * Exemplo:
   * - Assinatura está ativa (ativo: true)
   * - Chama toggleAtivo(id)
   * - Assinatura fica inativa (ativo: false)
   * - Chamando novamente, volta para ativa
   *
   * @param id: ID da assinatura a alternar (ex: 2)
   * @returns Observable<AssinaturaResponse> com ativo invertido
   *
   * Fluxo no SubscriptionsComponent:
   * 1. Usuário vê Netflix com badge "Ativa"
   * 2. Clica em play/pause
   * 3. Chama toggleAtivo(5)
   * 4. Envia PATCH /api/subscriptions/5/toggle
   * 5. Servidor inverte ativo (true → false ou false → true)
   * 6. Retorna assinatura com novo estado
   * 7. Card atualiza para mostrar "Inativa"
   *
   * HTTP Method PATCH:
   * - PATCH = atualizar um campo específico
   * - Corpo vazio {} (servidor já sabe o que fazer)
   * - Apenas inverte o boolean ativo
   *
   * Benefício vs Deletar:
   * - Não perde histórico (assinatura ainda existe no BD)
   * - Usuário pode reativar depois
   * - Mais flexível que deletar permanentemente
   */
  toggleAtivo(id: number): Observable<AssinaturaResponse> {
    return this.http.patch<AssinaturaResponse>(`${this.apiUrl}/${id}/toggle`, {});
  }

  /**
   * resumoFinanceiro() — GET /api/subscriptions/resumo
   *
   * Propósito: Obter análise financeira completa do usuário
   *
   * @returns Observable<ResumoFinanceiro> com:
   *   - salario: salário do usuário
   *   - totalMensal: soma de todas as assinaturas ativas
   *   - percentualDoSalario: (totalMensal / salario) * 100
   *   - assinaturas: array com todas (ativas + inativas)
   *   - gastosPorCategoria: { categoria: total, ... }
   *
   * Fluxo no DashboardComponent:
   * 1. ngOnInit() chama resumoFinanceiro()
   * 2. Servidor processa:
   *    - Busca todas as assinaturas do usuário
   *    - Calcula soma (apenas ativas)
   *    - Calcula percentual
   *    - Agrupa por categoria
   * 3. Retorna objeto ResumoFinanceiro
   * 4. Dashboard exibe:
   *    - 3 cards (total assinaturas, gasto mensal, percentual)
   *    - Barra de progresso
   *    - Gráfico de categorias
   *
   * Por que no servidor e não no frontend?
   * - Servidor tem dados frescos do BD
   * - Evita cálculos errados no frontend
   * - Lógica centralizada (fácil de mudar)
   */
  resumoFinanceiro(): Observable<ResumoFinanceiro> {
    return this.http.get<ResumoFinanceiro>(`${this.apiUrl}/resumo`);
  }
}
