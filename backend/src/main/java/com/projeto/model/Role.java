package com.projeto.model;

/**
 * Enum que representa os papéis (roles/perfis) de acesso disponíveis no sistema.
 *
 * O que é um enum?
 * → Enum (enumeration = enumeração) é um tipo especial em Java que define um conjunto
 *   FIXO e LIMITADO de constantes. É como uma lista fechada de valores permitidos.
 *
 * Por que usar enum em vez de String?
 * → Segurança em tempo de compilação: se você tentar usar um valor inexistente
 *   (ex: Role.SUPERUSUARIO), o compilador vai reclamar ANTES de o programa rodar.
 * → Com String, um erro de digitação ("ADIMIN" em vez de "ADMIN") só seria detectado
 *   em tempo de execução, podendo causar bugs difíceis de rastrear.
 * → O código fica mais legível e autodocumentado — ao ler "Role.ADMIN" fica claro
 *   que estamos falando de um papel de administrador, sem ambiguidade.
 *
 * Como o JPA armazena enums no banco de dados?
 * → Por padrão, o JPA pode armazenar como número (ORDINAL: USER=0, ADMIN=1)
 *   ou como texto (STRING: "USER", "ADMIN").
 * → Neste projeto usamos @Enumerated(EnumType.STRING) nas entidades que referenciam
 *   este enum, então o banco armazena o texto "USER" ou "ADMIN".
 * → Isso é MUITO melhor porque se adicionarmos um novo papel entre USER e ADMIN no futuro,
 *   os índices ORDINAL mudariam e corromperiam os dados já gravados no banco.
 *   Com STRING, cada valor é independente e não quebra com adições futuras.
 */
public enum Role {

    // Papel padrão atribuído automaticamente a qualquer novo usuário que se cadastra.
    // Um USER pode gerenciar apenas suas próprias assinaturas (criar, editar, remover).
    // NÃO tem acesso às rotas administrativas /api/admin/**
    USER,

    // Papel privilegiado, atribuído manualmente por outro administrador via endpoint específico.
    // Um ADMIN pode ver todos os usuários cadastrados, todas as assinaturas de qualquer usuário,
    // promover outros usuários para ADMIN e visualizar rankings globais.
    // Tem acesso completo a todas as rotas, incluindo /api/admin/**
    ADMIN
}
