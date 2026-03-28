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

export interface PlataformaOpcao {
  nome: string;
  categoria: CategoriaAssinatura;
}

export const PLATAFORMAS_PREDEFINIDAS: PlataformaOpcao[] = [
  // Streaming de Vídeo
  { nome: 'Netflix',                categoria: 'STREAMING_VIDEO' },
  { nome: 'Amazon Prime Video',     categoria: 'STREAMING_VIDEO' },
  { nome: 'Disney+',                categoria: 'STREAMING_VIDEO' },
  { nome: 'Max',                    categoria: 'STREAMING_VIDEO' },
  { nome: 'Globoplay',              categoria: 'STREAMING_VIDEO' },
  { nome: 'Paramount+',             categoria: 'STREAMING_VIDEO' },
  { nome: 'Apple TV+',              categoria: 'STREAMING_VIDEO' },
  { nome: 'Crunchyroll',            categoria: 'STREAMING_VIDEO' },
  { nome: 'Mubi',                   categoria: 'STREAMING_VIDEO' },
  { nome: 'Star+',                  categoria: 'STREAMING_VIDEO' },
  // Streaming de Música
  { nome: 'Spotify',                categoria: 'STREAMING_MUSICA' },
  { nome: 'Apple Music',            categoria: 'STREAMING_MUSICA' },
  { nome: 'Deezer',                 categoria: 'STREAMING_MUSICA' },
  { nome: 'YouTube Music',          categoria: 'STREAMING_MUSICA' },
  { nome: 'Amazon Music',           categoria: 'STREAMING_MUSICA' },
  { nome: 'Tidal',                  categoria: 'STREAMING_MUSICA' },
  // Inteligência Artificial
  { nome: 'ChatGPT Plus',           categoria: 'SOFTWARE' },
  { nome: 'Claude Pro',             categoria: 'SOFTWARE' },
  { nome: 'GitHub Copilot',         categoria: 'SOFTWARE' },
  { nome: 'Gemini Advanced',        categoria: 'SOFTWARE' },
  { nome: 'Perplexity Pro',         categoria: 'SOFTWARE' },
  { nome: 'Midjourney',             categoria: 'SOFTWARE' },
  // Jogos
  { nome: 'Xbox Game Pass',         categoria: 'JOGOS' },
  { nome: 'PlayStation Plus',       categoria: 'JOGOS' },
  { nome: 'Nintendo Switch Online', categoria: 'JOGOS' },
  { nome: 'EA Play',                categoria: 'JOGOS' },
  // Software
  { nome: 'Microsoft 365',          categoria: 'SOFTWARE' },
  { nome: 'Adobe Creative Cloud',   categoria: 'SOFTWARE' },
  { nome: 'Notion',                 categoria: 'SOFTWARE' },
  { nome: '1Password',              categoria: 'SOFTWARE' },
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
