import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {
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
