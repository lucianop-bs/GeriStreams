/**
 * home.component.ts — Componente da Página Inicial (Home)
 *
 * Responsabilidade: Exibir página de apresentação do GeriStreams.
 *
 * Conteúdo:
 * - Carrossel de serviços (Netflix, Spotify, etc)
 * - Texto de apresentação (hero section)
 * - Botões para Login/Register
 *
 * Este é um componente PÚBLICO:
 * - Pode acessar sem estar logado
 * - Sem AuthGuard na rota
 *
 * Propósito:
 * - Primeira impressão dos usuários
 * - Apresentar valor do GeriStreams
 * - Incentivar cadastro/login
 */

import {Component} from '@angular/core';

// RouterLink: habilita [routerLink] no template (navegação programática)
import {RouterLink} from '@angular/router';

/**
 * Service: interface que representa um serviço no carrossel
 *
 * Campos:
 * - name: nome do serviço (ex: "Netflix")
 * - icon: classe Bootstrap Icons (ex: "bi-play-circle-fill")
 * - color: cor da marca (hexadecimal, ex: "#E50914" = vermelho Netflix)
 * - bg: cor de fundo do card no carrossel
 *
 * Nota: Esta é uma interface apenas visual, não está no models/
 * porque é específica deste componente
 */

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {

  /**
   * services: Array de serviços para exibir no carrossel
   *
   * Por que array?
   * - template usa @for para iterar: @for (service of services)
   * - cada item gera um slide do carrossel
   *
   * Cores escolhidas:
   * - Cada cor (color, bg) é a cor oficial da marca
   * - Isso torna visual reconhecível e bonito
   *
   * Bootstrap Icons (bi-* classes):
   * - Ícones gratuitos do Bootstrap Icons
   * - Ex: bi-play-circle-fill = círculo de play
   * - Completo em: https://icons.getbootstrap.com/
   *
   * Serviços incluídos:
   * - Netflix, Spotify, Disney+: mais populares
   * - Amazon Prime, YouTube Premium: alternativas
   * - HBO Max, Apple TV+, Crunchyroll: streaming específicos
   * - ChatGPT Plus, Copilot: IA (tendência moderna)
   *
   * Nota:
   * - Não é lista completa (seria muito grande)
   * - Representa diversidade de serviços
   * - Usuário pode adicionar outros no dashboard
   */
  services = [
    { name: 'Netflix',         icon: 'bi-play-circle-fill',   color: '#E50914', bg: '#141414' },
    { name: 'Spotify',         icon: 'bi-music-note-beamed',  color: '#1DB954', bg: '#191414' },
    { name: 'Disney+',         icon: 'bi-stars',              color: '#113CCF', bg: '#040B1E' },
    { name: 'Amazon Prime',    icon: 'bi-box-seam-fill',      color: '#00A8E1', bg: '#232F3E' },
    { name: 'YouTube Premium', icon: 'bi-youtube',            color: '#FF0000', bg: '#282828' },
    { name: 'HBO Max',         icon: 'bi-film',               color: '#5822B4', bg: '#1A1A2E' },
    { name: 'Apple TV+',       icon: 'bi-apple',              color: '#A2AAAD', bg: '#1C1C1E' },
    { name: 'ChatGPT Plus',    icon: 'bi-robot',              color: '#10A37F', bg: '#202123' },
    { name: 'Copilot',         icon: 'bi-code-slash',         color: '#7B61FF', bg: '#1B1D23' },
    { name: 'Crunchyroll',     icon: 'bi-camera-reels-fill',  color: '#F47521', bg: '#1A1A1A' },
  ];
}
