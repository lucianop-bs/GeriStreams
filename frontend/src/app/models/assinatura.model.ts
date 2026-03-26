/**
 * assinatura.model.ts — Modelos de Dados de Assinaturas/Subscriptions
 *
 * Define as interfaces e tipos para representar assinaturas (serviços como
 * Netflix, Spotify, etc) que os usuários contratam e gerenciam no sistema.
 */

/**
 * CategoriaAssinatura — Enum de categorias disponíveis
 *
 * Um "type" (tipo) enumerado define um conjunto FECHADO de valores permitidos.
 * Se você tentar usar a string 'VIDEOGAME' (que não está na lista),
 * o TypeScript reclama.
 *
 * Categorias disponíveis:
 * - STREAMING_VIDEO: Netflix, Disney+, etc
 * - STREAMING_MUSICA: Spotify, Apple Music, etc
 * - JOGOS: Xbox Game Pass, PlayStation Plus, etc
 * - SOFTWARE: Adobe Creative Cloud, Microsoft 365, etc
 * - NOTICIAS: Jornal online, revistas, etc
 * - OUTRO: Qualquer coisa que não se encaixa acima
 *
 * Benefício: Type safety
 * - Sem type union, você poderia enviar qualquer string
 * - Com type union, apenas valores permitidos são aceitos
 */
export type CategoriaAssinatura =
  | 'STREAMING_VIDEO'
  | 'STREAMING_MUSICA'
  | 'JOGOS'
  | 'SOFTWARE'
  | 'NOTICIAS'
  | 'OUTRO';

/**
 * CATEGORIAS — Array com todas as categorias e seus rótulos legíveis
 *
 * Esta é uma CONSTANTE (não muda durante a execução) que mapeia cada
 * categoria técnica (STREAMING_VIDEO) para um rótulo legível (Streaming de Vídeo).
 *
 * Uso principal:
 * - Preencher <select> no formulário de assinaturas
 * - Converter valor técnico em texto legível para exibição
 *
 * Estrutura:
 * - value: código técnico (salvo no banco de dados)
 * - label: texto legível para o usuário (mostrado na tela)
 *
 * Uso em template:
 * @for (cat of categorias; track cat.value) {
 *   <option [value]="cat.value">{{ cat.label }}</option>
 * }
 *
 * Uso em TypeScript:
 * const label = CATEGORIAS.find(c => c.value === 'STREAMING_VIDEO')?.label;
 * // Resultado: "Streaming de Vídeo"
 */
export const CATEGORIAS: { value: CategoriaAssinatura; label: string }[] = [
  { value: 'STREAMING_VIDEO',  label: 'Streaming de Vídeo' },
  { value: 'STREAMING_MUSICA', label: 'Streaming de Música' },
  { value: 'JOGOS',            label: 'Jogos' },
  { value: 'SOFTWARE',         label: 'Software' },
  { value: 'NOTICIAS',         label: 'Notícias' },
  { value: 'OUTRO',            label: 'Outro' },
];

/**
 * AssinaturaRequest — Dados que o cliente ENVIA para criar/atualizar assinatura
 *
 * Campo       | Tipo                    | Descrição
 * ------------|-------------------------|-------------------------------------------
 * nome        | string                  | Nome do serviço (ex: "Netflix")
 * valor       | number                  | Valor mensal em reais (ex: 49.90)
 * categoria   | CategoriaAssinatura     | Categoria do serviço
 *
 * Exemplo:
 * const novaAssinatura: AssinaturaRequest = {
 *   nome: 'Netflix',
 *   valor: 49.90,
 *   categoria: 'STREAMING_VIDEO'
 * };
 */
export interface AssinaturaRequest {
  nome: string;
  valor: number;
  categoria: CategoriaAssinatura;
}

/**
 * AssinaturaResponse — Dados que o servidor RETORNA sobre uma assinatura
 *
 * Campo       | Tipo                | Descrição
 * ------------|---------------------|-------------------------------------------
 * id          | number              | ID único da assinatura
 *             |                     | - Gerado pelo servidor
 *             |                     | - Usado para editar/deletar
 * nome        | string              | Nome do serviço
 * valor       | number              | Valor mensal
 * categoria   | CategoriaAssinatura | Categoria do serviço
 * ativo       | boolean             | Está ativa ou pausada?
 *             |                     | - true: cobrando mensalmente
 *             |                     | - false: pausada (usuário não quer usar)
 * createdAt   | string              | Data de criação
 *             |                     | - Formato ISO (ex: "2026-03-25T10:30:00Z")
 *
 * Exemplo:
 * {
 *   id: 5,
 *   nome: 'Netflix',
 *   valor: 49.90,
 *   categoria: 'STREAMING_VIDEO',
 *   ativo: true,
 *   createdAt: '2026-03-25T10:30:00Z'
 * }
 */
export interface AssinaturaResponse {
  id: number;
  nome: string;
  valor: number;
  categoria: CategoriaAssinatura;
  ativo: boolean;
  createdAt: string;
}
