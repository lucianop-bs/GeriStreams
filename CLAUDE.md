# Contexto do Projeto: Sistema Web Fullstack (Spring Boot + Angular)

## 1. Visão Geral
Desenvolvimento de um sistema web do zero utilizando arquitetura moderna de SPA (Single Page Application) integrada a uma API REST robusta.

## 2. Stack Tecnológica
- **Frontend:** Angular (v21)  usando o signals com TypeScript.
- **UI/Styling:** Bootstrap 5 para responsividade e componentes prontos.
- **Backend:** Java com Spring Boot 3 (Spring Web, Spring Data JPA, Spring Security).
- **Banco de Dados:** PostgreSQL.
- **Comunicação:** JSON via REST API.

## 3. Arquitetura e Padrões 
Mesmo usando frameworks, o projeto deve respeitar a separação de camadas exigida:
- **Entity:** Mapeamento JPA.
- **Repository:** Interface Spring Data para persistência (equivalente ao DAO).
- **Service (BO):** Camada de serviço contendo TODA a lógica de negócio e validações.
- **DTO:** Objetos de transferência de dados. **Nunca** expor as Entities nos Controllers.
- **Controller:** Endpoints REST.

## 4. Estrutura de Pastas Esperada
### Backend (Spring Boot)
- `src/main/java/com/projeto/controller/`
- `src/main/java/com/projeto/service/`
- `src/main/java/com/projeto/repository/`
- `src/main/java/com/projeto/dto/`
- `src/main/java/com/projeto/model/` (Entities)

### Frontend (Angular)
- `src/app/components/` (UI)
- `src/app/services/` (Comunicação com API)
- `src/app/models/` (Interfaces/Classes DTO)
- `src/app/guards/` (Proteção de rotas)

## 5. Regras para a IA
- Siga as convenções de código `PascalCase` para classes e `camelCase` para métodos/variáveis.
- O código deve ser limpo e pronto para ser explicado em uma arguição técnica.
- Use o Bootstrap para garantir que a interface seja profissional e funcional.