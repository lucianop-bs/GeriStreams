---
globs: src/api/**/*.ts, src/routes/**/*.ts, src/middlewares/**/*.ts
---

# Regras da API e Fluxo Global

## Interceptadores e Middlewares
- **Erros Globais:** Todos os erros devem ser capturados por um middleware de erro global (`ErrorHandler`) em `src/middlewares/error.ts`.
- **Interceptadores:** Use interceptadores globais para transformar a resposta da API antes de enviá-la ao cliente (ex: padronização de data/hora).
- **Proibição:** É proibido usar blocos `try/catch` vazios ou que não repassem o erro para o `next(error)` (em Express) ou lancem uma `Exception` tratada.

## Padrões de Resposta
- **Formato:** Retorne erros sempre no formato `{ error: string, code: number }`.
- **Autenticação:** Use middleware de autenticação obrigatório em rotas que não sejam públicas.

## Observabilidade e Documentação
- **Logging:** Sempre logue erros usando o logger interno (`logger.error()`). **Nunca use `console.log`**.
- **OpenAPI:** Todo novo endpoint ou alteração de contrato deve ser refletido no arquivo de especificação OpenAPI/Swagger.

##  Regras para Geração de Código
1. Siga o padrão de nomenclatura Java (camelCase para métodos/variáveis, PascalCase para classes).
2. O código deve ser modular e documentado para facilitar a explicação na arguição oral.
3. Use Bootstrap para garantir uma interface profissional e responsiva.
4. Ao criar novas funcionalidades, gere sempre a Migration SQL correspondente.
5. Neste projeto, é terminantemente proibido o uso de *ngIf, *ngFor ou qualquer atributo depreciado do Angular.
6. Adotaremos exclusivamente o Built-in Control Flow (@if, @for, @switch) e a gestão de estado via Signals, visando a máxima performance e reatividade granular.
