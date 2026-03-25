/**
 * app.config.ts — Configuração raiz da aplicação Angular (Standalone API)
 *
 * Este arquivo é o ponto central de configuração do Angular moderno (v17+).
 * Na arquitetura antiga (antes do Angular 14), toda a configuração era feita
 * dentro de um NgModule chamado AppModule (arquivo app.module.ts).
 * Com a "Standalone API", não precisamos mais de um NgModule raiz.
 * Em vez disso, usamos este objeto de configuração que é passado diretamente
 * para a função bootstrapApplication() no arquivo main.ts.
 *
 * Pense neste arquivo como o "painel de controle" da aplicação:
 * aqui registramos todos os serviços globais, o roteador, o cliente HTTP
 * e os interceptores que vão funcionar em toda a aplicação.
 */

// ApplicationConfig: é um tipo (interface TypeScript) definido pelo Angular
// que descreve o formato esperado do objeto de configuração. Ele exige que
// tenhamos uma propriedade chamada "providers", que é um array de provedores
// que serão disponibilizados em toda a aplicação (injetor raiz).
//
// provideBrowserGlobalErrorListeners: função introduzida no Angular 19 que
// registra listeners globais de erro no browser, especificamente:
//   - window.onerror: captura erros JavaScript síncronos não tratados
//   - window.onunhandledrejection: captura Promises rejeitadas sem .catch()
// Isso melhora a observabilidade da aplicação, pois erros que "escapam"
// do sistema do Angular também são capturados.
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';

// provideRouter: função que substitui o antigo "RouterModule.forRoot(routes)"
// que era importado dentro do AppModule. Na Standalone API, em vez de importar
// um módulo, chamamos esta função de "provedor" que registra internamente o
// Router, ActivatedRoute e todos os serviços de roteamento como injetáveis
// globais. Recebe o array de rotas como argumento obrigatório.
import { provideRouter } from '@angular/router';

// provideHttpClient: substitui o antigo "HttpClientModule" que precisava ser
// importado no AppModule. Registra o HttpClient como um serviço disponível
// para injeção em toda a aplicação.
//
// withInterceptors: é uma "feature" (característica adicional) do provideHttpClient.
// Ela permite registrar interceptores HTTP na forma funcional moderna (funções
// simples, sem precisar criar classes que implementam HttpInterceptor).
// Recebe um array de funções interceptoras.
import { provideHttpClient, withInterceptors } from '@angular/common/http';

// Importa o array de rotas definido no arquivo app.routes.ts.
// Esse array mapeia cada URL para um componente específico.
import { routes } from './app.routes';

// jwtInterceptor: intercepta TODAS as requisições HTTP que saem da aplicação
// e adiciona automaticamente o token JWT no cabeçalho "Authorization".
// Isso evita ter que adicionar o token manualmente em cada chamada de serviço.
import { jwtInterceptor } from './interceptors/jwt.interceptor';

// errorInterceptor: intercepta TODAS as respostas HTTP que chegam da API
// e trata erros globais, como:
//   - 401 (Não Autorizado): token expirado → faz logout automático
//   - 403 (Proibido): sem permissão → redireciona para dashboard
import { errorInterceptor } from './interceptors/error.interceptor';

/**
 * appConfig: o objeto de configuração exportado que será consumido no main.ts.
 * A propriedade "providers" é um array onde cada item é um "provedor" —
 * uma instrução para o sistema de injeção de dependência do Angular sobre
 * como criar e disponibilizar um determinado serviço ou funcionalidade.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    // Registra os listeners globais de erro do browser.
    // Garante que erros JavaScript não capturados pelo Angular também sejam
    // monitorados, melhorando a robustez da aplicação em produção.
    provideBrowserGlobalErrorListeners(),

    // Registra o sistema de roteamento com as rotas da nossa aplicação.
    // Sem este provedor, clicar em links ou navegar por URL não funcionaria,
    // pois o Angular não saberia qual componente exibir para cada endereço.
    provideRouter(routes),

    // Registra o HttpClient (cliente HTTP) e configura os interceptores.
    //
    // A ORDEM DOS INTERCEPTORES DENTRO DO ARRAY É CRÍTICA:
    //
    // Para requisições SAINDO (request pipeline):
    //   Os interceptores executam na ORDEM DA LISTA, do primeiro ao último.
    //   Então: jwtInterceptor roda ANTES do errorInterceptor.
    //   Isso é correto porque queremos adicionar o token JWT antes de enviar.
    //
    // Para respostas CHEGANDO (response pipeline):
    //   Os interceptores executam na ORDEM INVERSA, do último ao primeiro.
    //   Então: errorInterceptor roda ANTES do jwtInterceptor na resposta.
    //   Isso é correto porque queremos tratar erros (401, 403) assim que
    //   a resposta chega, antes de qualquer outro processamento.
    //
    // Resumo do fluxo:
    //   [Componente] → jwtInterceptor (adiciona token) → errorInterceptor → [API]
    //   [API] → errorInterceptor (trata erros) → jwtInterceptor → [Componente]
    provideHttpClient(withInterceptors([jwtInterceptor, errorInterceptor]))
  ]
};
