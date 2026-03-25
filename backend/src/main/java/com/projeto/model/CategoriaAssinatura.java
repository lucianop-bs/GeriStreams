package com.projeto.model;

/**
 * Enum que define as categorias possíveis para uma assinatura digital.
 *
 * Por que usar enum para categorias?
 * → Garante consistência dos dados: somente valores definidos aqui podem ser salvos no banco.
 *   Sem enum, alguém poderia salvar "Streaming_Video", "streaming_video", "STREAMING VIDEO"
 *   (com espaço) e todos seriam tratados como valores diferentes, bagunçando filtros e relatórios.
 * → Facilita a exibição no frontend: o Angular recebe o nome do enum (ex: "STREAMING_VIDEO")
 *   e pode mapear para um texto amigável ("Streaming de Vídeo") de forma centralizada.
 * → Facilita agrupamentos e rankings: no relatório financeiro, agrupamos gastos por categoria.
 *   Com enum, a query JPQL com GROUP BY funciona corretamente porque os valores são uniformes.
 *
 * Como o JPA armazena este enum?
 * → Com @Enumerated(EnumType.STRING) na entidade Assinatura, o JPA grava o nome do enum
 *   como String no banco (ex: "STREAMING_VIDEO"), não como número.
 *   Isso protege contra corrupção de dados em caso de reordenação das constantes.
 */
public enum CategoriaAssinatura {

    // Serviços de vídeo sob demanda.
    // Exemplos: Netflix, Disney+, Amazon Prime Video, HBO Max, Globoplay, Apple TV+
    STREAMING_VIDEO,

    // Serviços de música e podcasts digitais.
    // Exemplos: Spotify, Apple Music, Deezer, Amazon Music, YouTube Music
    STREAMING_MUSICA,

    // Serviços de jogos por assinatura.
    // Exemplos: Xbox Game Pass, PlayStation Plus, EA Play, Nintendo Switch Online
    JOGOS,

    // Softwares e ferramentas pagas com modelo de assinatura (SaaS - Software as a Service).
    // Exemplos: Adobe Creative Cloud, Microsoft 365, Notion, Figma, LastPass
    SOFTWARE,

    // Jornais, revistas e portais de notícias com paywall (acesso pago).
    // Exemplos: The New York Times, Folha de S.Paulo, Estadão, The Economist
    NOTICIAS,

    // Categoria genérica para qualquer assinatura que não se encaixe nas categorias acima.
    // Exemplos: clubes de assinatura, academia online, plataformas de ensino, etc.
    OUTRO
}
