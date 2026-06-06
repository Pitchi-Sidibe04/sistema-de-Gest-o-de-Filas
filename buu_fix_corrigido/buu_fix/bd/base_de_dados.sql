CREATE DATABASE IF NOT EXISTS sistema_senhas;
USE sistema_senhas;

-- ==========================
-- NÍVEIS DE ACESSO
-- ==========================
CREATE TABLE nivel_acesso (
    id_nivel INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE
);

-- ==========================
-- UTILIZADORES
-- ==========================
CREATE TABLE utilizador (
    id_utilizador INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    estado ENUM('ATIVO', 'INATIVO') DEFAULT 'ATIVO',
    id_nivel INT NOT NULL,
    FOREIGN KEY (id_nivel)
        REFERENCES nivel_acesso(id_nivel)
);

-- ==========================
-- BALCÕES
-- ==========================
CREATE TABLE balcao (
    id_balcao INT AUTO_INCREMENT PRIMARY KEY,
    numero_balcao INT NOT NULL UNIQUE,
    estado ENUM('ATIVO', 'INATIVO') DEFAULT 'ATIVO'
);

-- ==========================
-- SERVIÇOS
-- ==========================
CREATE TABLE servico (
    id_servico INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    letra CHAR(1) NOT NULL UNIQUE,
    prioritario BOOLEAN DEFAULT FALSE
);

-- ==========================
-- SENHAS
-- ==========================
CREATE TABLE senha (
    id_senha INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    id_servico INT NOT NULL,

    estado ENUM(
        'EM_ESPERA',
        'CHAMADA',
        'EM_ATENDIMENTO',
        'CONCLUIDA',
        'CANCELADA'
    ) DEFAULT 'EM_ESPERA',

    numero_chamadas INT DEFAULT 0,

    data_emissao DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_servico)
        REFERENCES servico(id_servico)
);

-- ==========================
-- ATENDIMENTOS
-- ==========================
CREATE TABLE atendimento (
    id_atendimento INT AUTO_INCREMENT PRIMARY KEY,

    id_senha INT NOT NULL,
    id_balcao INT NOT NULL,
    id_utilizador INT NOT NULL,

    hora_chamada DATETIME,
    hora_inicio DATETIME,
    hora_fim DATETIME,

    observacao TEXT,

    FOREIGN KEY (id_senha)
        REFERENCES senha(id_senha),

    FOREIGN KEY (id_balcao)
        REFERENCES balcao(id_balcao),

    FOREIGN KEY (id_utilizador)
        REFERENCES utilizador(id_utilizador)
);

-- ==========================
-- LOG DE ATIVIDADES
-- ==========================
CREATE TABLE log_atividade (
    id_log INT AUTO_INCREMENT PRIMARY KEY,

    id_utilizador INT NOT NULL,

    acao VARCHAR(150) NOT NULL,

    data_hora DATETIME DEFAULT CURRENT_TIMESTAMP,

    descricao TEXT,

    FOREIGN KEY (id_utilizador)
        REFERENCES utilizador(id_utilizador)
);

-- ==========================
-- DADOS INICIAIS
-- ==========================

INSERT INTO nivel_acesso(nome)
VALUES
('Administrador'),
('Supervisor'),
('Atendente');

INSERT INTO servico(nome, letra, prioritario)
VALUES
('Caixa', 'A', FALSE),
('Conta', 'B', FALSE),
('Cartões', 'C', FALSE),
('Atendimento Prioritário', 'D', TRUE),
('Outros Serviços', 'E', FALSE);

INSERT INTO balcao(numero_balcao)
VALUES
(1),
(2),
(3),
(4),
(5);