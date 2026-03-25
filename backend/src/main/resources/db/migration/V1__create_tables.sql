-- V1: Criação das tabelas principais

CREATE TABLE usuarios (
    id         BIGSERIAL    PRIMARY KEY,
    nome       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    senha      VARCHAR(255) NOT NULL,
    salario    NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE assinaturas (
    id         BIGSERIAL    PRIMARY KEY,
    nome       VARCHAR(100) NOT NULL,
    valor      NUMERIC(10,2) NOT NULL,
    categoria  VARCHAR(50)  NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    usuario_id BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
