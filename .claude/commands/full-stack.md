Você é o orquestrador de features fullstack do GeriStreams. Use o agente `full-stack-feature` para criar uma feature completa.

## O que este comando faz

Cria uma feature completa do zero, cobrindo todas as camadas:

**Backend (Spring Boot 3):**
1. Entity JPA com todas as anotações corretas
2. Migration Flyway (próxima versão V automaticamente detectada)
3. Repository (Spring Data JPA)
4. Request DTO (Java record + Bean Validation)
5. Response DTO (com método `fromEntity()`)
6. Service (lógica de negócio, `@Transactional`, logger Slf4j)
7. Controller (endpoints REST, `@Valid`, DTOs, OpenAPI annotations)

**Frontend (Angular 21):**
8. Model TypeScript (interfaces correspondentes ao backend)
9. Service Angular (`inject(HttpClient)`, `environment.apiUrl`)
10. Component standalone com Signals e `@if/@for`
11. Rota com `loadComponent` em `app.routes.ts`

## Como usar

Descreva a feature que deseja criar. Exemplos:

- "Criar feature de notificações para alertar usuários sobre assinaturas expirando"
- "Adicionar módulo de metas de economia financeira"
- "Criar sistema de tags para categorizar assinaturas"
- "Adicionar histórico de alterações nas assinaturas"

## Regras que serão aplicadas automaticamente

- PROIBIDO: `*ngIf`, `*ngFor`, expor Entity em Controller, `System.out.println`, `console.log`
- OBRIGATÓRIO: Migration SQL por feature, `@Valid` nos controllers, `@Transactional` nas mutations, Signals no Angular, `standalone: true` nos componentes

## Passo 1 — Descobrir estado atual

Antes de gerar qualquer código:
1. Listar migrations existentes para determinar o próximo número V
2. Ler uma Entity e DTO existentes como referência de estilo

## Passo 2 — Gerar todos os arquivos em ordem

Seguir a ordem exata: Entity → Migration → Repository → DTOs → Service → Controller → Model TS → Service Angular → Component → Rota

## Passo 3 — Validação final

Após gerar todos os arquivos, verificar:
- [ ] Número da migration está correto
- [ ] SQL alinhado com JPA Entity
- [ ] Nenhuma Entity exposta no Controller
- [ ] Signals usados no componente Angular
- [ ] Rota adicionada em `app.routes.ts`

Forneça agora a descrição da feature que deseja criar:
