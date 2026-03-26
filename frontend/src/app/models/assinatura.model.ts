export type CategoriaAssinatura =
  | 'STREAMING_VIDEO'
  | 'STREAMING_MUSICA'
  | 'JOGOS'
  | 'SOFTWARE'
  | 'NOTICIAS'
  | 'OUTRO';

export const CATEGORIAS: { value: CategoriaAssinatura; label: string }[] = [
  { value: 'STREAMING_VIDEO',  label: 'Streaming de Vídeo' },
  { value: 'STREAMING_MUSICA', label: 'Streaming de Música' },
  { value: 'JOGOS',            label: 'Jogos' },
  { value: 'SOFTWARE',         label: 'Software' },
  { value: 'NOTICIAS',         label: 'Notícias' },
  { value: 'OUTRO',            label: 'Outro' },
];

export interface AssinaturaRequest {
  nome: string;
  valor: number;
  categoria: CategoriaAssinatura;
}

export interface AssinaturaResponse {
  id: number;
  nome: string;
  valor: number;
  categoria: CategoriaAssinatura;
  ativo: boolean;
  createdAt: string;
}
