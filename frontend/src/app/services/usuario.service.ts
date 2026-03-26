/**
 * usuario.service.ts — Serviço de Requisições de Usuário
 *
 * Responsabilidade: Fazer comunicação HTTP com endpoints relacionados a usuários.
 *
 * Operações:
 * - Buscar perfil do usuário logado
 * - Atualizar salário
 * - (ADMIN) Listar todos os usuários
 * - (ADMIN) Buscar usuário específico
 * - (ADMIN) Listar assinaturas de um usuário
 * - (ADMIN) Promover usuário para admin
 *
 * Padrão Observable:
 * Cada método retorna Observable<T> (não a resposta diretamente).
 * Isso permite que o componente controle quando e como inscrever-se.
 */

import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {AtualizarSalario, Usuario} from '../models/usuario.model';
import {AssinaturaResponse} from '../models/assinatura.model';
import {environment} from '../../environments/environment';

/**
 * @Injectable: Marca esta classe como "injetável"
 * Significa que o Angular pode fornecer uma instância desta classe
 * para qualquer componente/serviço que pedir.
 *
 * providedIn: 'root': Define que há apenas UMA instância deste serviço
 * em toda a aplicação (Singleton pattern).
 * Alternativa: providedIn: FeatureModule (múltiplas instâncias em módulos)
 */
@Injectable({ providedIn: 'root' })
export class UsuarioService {

  // URL base da API (definida em environment.ts)
  // Exemplo: 'http://localhost:8080/api'
  // Cada método concatena endpoints específicos
  private readonly apiUrl = `${environment.apiUrl}/api`;

  /**
   * CONSTRUTOR
   *
   * @param http: HttpClient injetado para fazer requisições HTTP
   */
  constructor(private http: HttpClient) {}

  // ================= OPERAÇÕES DE USUÁRIO LOGADO =================

  /**
   * buscarPerfil() — GET /api/users/me
   *
   * Propósito: Buscar dados do usuário atualmente logado
   *
   * Endpoint: GET /api/users/me
   * - "/me" é um padrão REST que significa "o usuário logado"
   * - O servidor sabe quem é o usuário pelo token JWT no header Authorization
   *
   * @returns Observable<Usuario> com dados: id, nome, email, salario, role, createdAt
   *
   * Uso no DashboardComponent:
   * ```typescript
   * this.usuarioService.buscarPerfil().subscribe(usuario => {
   *   console.log('Usuário:', usuario.nome);
   * });
   * ```
   */
  buscarPerfil(): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.apiUrl}/users/me`);
  }

  /**
   * atualizarSalario() — PUT /api/users/me/salario
   *
   * Propósito: Atualizar o salário do usuário logado
   *
   * @param payload: { salario: number } — novo valor de salário
   * @returns Observable<Usuario> com dados atualizados
   *
   * Fluxo:
   * 1. Usuário preenche novo salário no DashboardComponent
   * 2. Envia payload: { salario: 3500 }
   * 3. Servidor atualiza salário no banco de dados
   * 4. Retorna usuário com novo salário
   * 5. Frontend atualiza tela
   *
   * Método HTTP:
   * - GET: buscar dados (não modifica)
   * - POST: criar novo recurso
   * - PUT: atualizar recurso inteiro
   * - PATCH: atualizar parte de um recurso
   * Neste caso, PUT é correto porque estamos atualizando "users/me"
   */
  atualizarSalario(payload: AtualizarSalario): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.apiUrl}/users/me/salario`, payload);
  }

  // ================= OPERAÇÕES ADMIN =================

  /**
   * listarTodos() — GET /api/admin/users
   *
   * Propósito: Listar TODOS os usuários cadastrados no sistema
   * Acesso: Apenas ADMIN (protegido por adminGuard na rota)
   *
   * @returns Observable<Usuario[]> array com todos os usuários
   *
   * Uso no AdminComponent:
   * ```typescript
   * this.usuarioService.listarTodos().subscribe(usuarios => {
   *   this.usuarios = usuarios; // Mostra lista na tela
   * });
   * ```
   */
  listarTodos(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.apiUrl}/admin/users`);
  }

  /**
   * buscarPorId() — GET /api/admin/users/{id}
   *
   * Propósito: Buscar dados de um usuário específico
   * Acesso: Apenas ADMIN
   *
   * @param id: número do usuário (ex: 5)
   * @returns Observable<Usuario> com dados do usuário
   *
   * Nota: Este método não é usado no projeto atual,
   * mas está disponível para futuras implementações.
   */
  buscarPorId(id: number): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.apiUrl}/admin/users/${id}`);
  }

  /**
   * listarAssinaturasDoUsuario() — GET /api/admin/users/{id}/subscriptions
   *
   * Propósito: Listar todas as assinaturas de um usuário específico
   * Acesso: Apenas ADMIN
   *
   * @param id: ID do usuário (ex: 3)
   * @returns Observable<AssinaturaResponse[]> array com assinaturas do usuário
   *
   * Fluxo no AdminComponent:
   * 1. Admin clica em um usuário na lista
   * 2. Chama verAssinaturas(usuario)
   * 3. Faz GET /api/admin/users/{usuario.id}/subscriptions
   * 4. Mostra assinaturas do lado direito da tela
   *
   * Uso:
   * ```typescript
   * this.usuarioService.listarAssinaturasDoUsuario(5).subscribe(assinaturas => {
   *   this.assinaturasSelecionadas = assinaturas;
   * });
   * ```
   */
  listarAssinaturasDoUsuario(id: number): Observable<AssinaturaResponse[]> {
    return this.http.get<AssinaturaResponse[]>(`${this.apiUrl}/admin/users/${id}/subscriptions`);
  }

  /**
   * promoverParaAdmin() — PATCH /api/admin/users/{id}/promote
   *
   * Propósito: Promover um usuário normal (USER) para administrador (ADMIN)
   * Acesso: Apenas ADMIN
   * Use Case: UC15
   *
   * @param id: ID do usuário a promover (ex: 2)
   * @returns Observable<Usuario> com role atualizado para 'ADMIN'
   *
   * Fluxo no AdminComponent:
   * 1. Admin vê lista de usuários
   * 2. Clica em botão "Promover" ao lado do usuário
   * 3. Pede confirmação: "Promover João para ADMIN?"
   * 4. Se confirma: chama promoverParaAdmin(usuario.id)
   * 5. Servidor atualiza role de 'USER' para 'ADMIN'
   * 6. Lista atualiza automaticamente
   *
   * Método HTTP:
   * - PATCH é usado porque estamos atualizando UM CAMPO (role)
   * - Não estamos atualizando o usuário inteiro
   *
   * Nota: Segurança importante
   * - Frontend pede confirmação ao admin
   * - Mas SERVIDOR valida se quem fez a requisição é admin
   * - Frontend pode ser burlado, backend não
   */
  promoverParaAdmin(id: number): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.apiUrl}/admin/users/${id}/promote`, {});
  }
}
