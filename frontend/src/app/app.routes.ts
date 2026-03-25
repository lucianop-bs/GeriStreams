/**
 * app.routes.ts — Definição de todas as rotas da aplicação
 *
 * Este arquivo mapeia cada URL (caminho) a um componente Angular.
 * É aqui que definimos a estrutura de navegação da aplicação:
 * quais páginas existem, quais precisam de autenticação, e como
 * o carregamento de cada componente é feito (lazy loading).
 *
 * O array "routes" exportado aqui é passado para provideRouter()
 * no arquivo app.config.ts, que registra o roteador do Angular.
 */

// Routes: é o tipo TypeScript que representa um array de configurações de rota.
// Cada elemento do array descreve uma rota, com propriedades como:
//   - path: o caminho da URL (ex: 'login', 'dashboard')
//   - loadComponent: carrega o componente de forma lazy (preguiçosa)
//   - canActivate: guards que controlam o acesso à rota
//   - redirectTo: redireciona para outro caminho automaticamente
import { Routes } from '@angular/router';

// authGuard: guard que verifica se o usuário está autenticado (tem token JWT).
// Se não estiver, redireciona para /login. Usado em rotas que exigem login.
import { authGuard } from './guards/auth.guard';

// adminGuard: guard que verifica se o usuário autenticado tem o papel de ADMIN.
// Se não for admin, redireciona para /dashboard. Usado apenas na rota /admin.
import { adminGuard } from './guards/admin.guard';

/**
 * routes: array de configuração de todas as rotas da aplicação.
 *
 * CONCEITO IMPORTANTE — Lazy Loading com loadComponent:
 * Em vez de importar todos os componentes no topo deste arquivo (import estático),
 * usamos "loadComponent" com import() dinâmico. Isso significa que o JavaScript
 * de cada componente só é baixado do servidor quando o usuário realmente acessa
 * aquela rota. O resultado é um bundle inicial muito menor e carregamento mais
 * rápido da aplicação.
 *
 * Exemplo: o JavaScript do AdminComponent não é carregado na inicialização da app.
 * Ele só é baixado quando o usuário navegar para /admin.
 *
 * A sintaxe: import('./components/login/login.component').then(m => m.LoginComponent)
 *   - import(): função nativa do JavaScript para importação dinâmica (lazy)
 *   - .then(m => m.LoginComponent): acessa a propriedade exportada do módulo
 */
export const routes: Routes = [
  // Rota raiz ('') — quando o usuário acessa apenas o domínio (ex: localhost:4200/).
  // redirectTo: 'dashboard' instrui o roteador a ir automaticamente para /dashboard.
  // pathMatch: 'full' é OBRIGATÓRIO aqui: significa que só redireciona se a URL
  // for EXATAMENTE '' (vazia). Sem 'full', o roteador redirecionaria para 'dashboard'
  // em QUALQUER URL que comece com '' — ou seja, todas as URLs — causando loop infinito.
  // Rota raiz — redireciona para a página inicial pública.
  { path: '', redirectTo: 'home', pathMatch: 'full' },

  // Rota Home — página pública de apresentação do sistema.
  // Exibe o carrossel de serviços, descrição e botões de acesso.
  {
    path: 'home',
    loadComponent: () => import('./components/home/home.component').then(m => m.HomeComponent)
  },

  // Rota de Login — página pública, não exige autenticação.
  // Qualquer usuário (logado ou não) pode acessar /login.
  // O componente é carregado de forma lazy: o JS só é baixado quando alguém
  // navegar para /login pela primeira vez.
  {
    path: 'login',
    loadComponent: () => import('./components/login/login.component').then(m => m.LoginComponent)
  },

  // Rota de Cadastro — página pública, não exige autenticação.
  // Permite que novos usuários se registrem no sistema.
  {
    path: 'register',
    loadComponent: () => import('./components/register/register.component').then(m => m.RegisterComponent)
  },

  // Rota do Dashboard — PROTEGIDA pelo authGuard.
  // canActivate: [authGuard] instrui o roteador a chamar o authGuard ANTES de
  // ativar (renderizar) esta rota. Se o guard retornar false (usuário não logado),
  // o roteador redireciona para /login automaticamente.
  // O componente é carregado de forma lazy.
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./components/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },

  // Rota de Assinaturas — PROTEGIDA pelo authGuard.
  // Apenas usuários autenticados podem ver e gerenciar suas assinaturas.
  {
    path: 'subscriptions',
    canActivate: [authGuard],
    loadComponent: () => import('./components/subscriptions/subscriptions.component').then(m => m.SubscriptionsComponent)
  },

  // Rota Admin — DUPLAMENTE PROTEGIDA por [authGuard, adminGuard].
  //
  // Por que dois guards e não apenas adminGuard?
  // O Angular executa os guards na ordem do array. Se colocássemos só [adminGuard],
  // o adminGuard precisaria verificar tanto autenticação quanto permissão de admin.
  // Com [authGuard, adminGuard], separamos as responsabilidades:
  //   1. authGuard (primeiro): garante que o usuário está logado. Se não estiver,
  //      redireciona para /login ANTES mesmo de verificar se é admin.
  //   2. adminGuard (segundo): só executa se authGuard passar. Verifica se o
  //      usuário logado tem papel ADMIN. Se não, redireciona para /dashboard.
  //
  // Isso segue o Princípio da Responsabilidade Única (SRP) e evita código duplicado.
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./components/admin/admin.component').then(m => m.AdminComponent)
  },

  // Rota curinga (wildcard) — captura QUALQUER caminho que não foi definido acima.
  // O '**' é um padrão que corresponde a qualquer URL.
  // Sempre deve ser a ÚLTIMA rota do array, pois o roteador testa as rotas em
  // ordem e para no primeiro match. Se estivesse no início, capturaria tudo.
  // Redireciona para /dashboard em vez de mostrar uma página 404 em branco.
  { path: '**', redirectTo: 'home' }
];
