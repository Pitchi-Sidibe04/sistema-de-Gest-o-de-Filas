-- ==============================================================
--  Banco UBUNTUU — Sistema de Gestão de Filas (SGF)
--  Script SQL completo e corrigido
-- ==============================================================

CREATE DATABASE IF NOT EXISTS sistema_senhas
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE sistema_senhas;

-- ==========================
-- NÍVEIS DE ACESSO
-- ==========================
CREATE TABLE IF NOT EXISTS nivel_acesso (
    id_nivel INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE
);

-- ==========================
-- UTILIZADORES
-- ==========================
CREATE TABLE IF NOT EXISTS utilizador (
    id_utilizador INT AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    senha         VARCHAR(255) NOT NULL,   -- BCrypt hash
    estado        ENUM('ATIVO','INATIVO') DEFAULT 'ATIVO',
    id_nivel      INT NOT NULL,
    data_criacao  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_nivel) REFERENCES nivel_acesso(id_nivel)
);

-- ==========================
-- BALCÕES
-- ==========================
CREATE TABLE IF NOT EXISTS balcao (
    id_balcao      INT AUTO_INCREMENT PRIMARY KEY,
    numero_balcao  INT NOT NULL UNIQUE,
    estado         ENUM('ATIVO','INATIVO') DEFAULT 'ATIVO'
);

-- ==========================
-- SERVIÇOS
-- ==========================
CREATE TABLE IF NOT EXISTS servico (
    id_servico  INT AUTO_INCREMENT PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    letra       CHAR(1)      NOT NULL UNIQUE,
    prioritario BOOLEAN      DEFAULT FALSE
);

-- ==========================
-- SENHAS
-- ==========================
CREATE TABLE IF NOT EXISTS senha (
    id_senha        INT AUTO_INCREMENT PRIMARY KEY,
    codigo          VARCHAR(20)  NOT NULL UNIQUE,
    id_servico      INT          NOT NULL,
    estado          ENUM('EM_ESPERA','CHAMADA','EM_ATENDIMENTO','CONCLUIDA','AUSENTE','CANCELADA')
                    DEFAULT 'EM_ESPERA',
    numero_chamadas INT          DEFAULT 0,
    data_emissao    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_servico) REFERENCES servico(id_servico),
    INDEX idx_estado      (estado),
    INDEX idx_data        (data_emissao),
    INDEX idx_serv_data   (id_servico, data_emissao)
);

-- ==========================
-- ATENDIMENTOS
-- ==========================
CREATE TABLE IF NOT EXISTS atendimento (
    id_atendimento INT AUTO_INCREMENT PRIMARY KEY,
    id_senha       INT  NOT NULL,
    id_balcao      INT  NOT NULL,
    id_utilizador  INT  NOT NULL,
    hora_chamada   DATETIME,
    hora_inicio    DATETIME,
    hora_fim       DATETIME,
    observacao     TEXT,
    FOREIGN KEY (id_senha)      REFERENCES senha(id_senha),
    FOREIGN KEY (id_balcao)     REFERENCES balcao(id_balcao),
    FOREIGN KEY (id_utilizador) REFERENCES utilizador(id_utilizador),
    INDEX idx_data_inicio (hora_inicio)
);

-- ==========================
-- LOG DE ACTIVIDADES
-- ==========================
CREATE TABLE IF NOT EXISTS log_atividade (
    id_log        INT AUTO_INCREMENT PRIMARY KEY,
    id_utilizador INT          NOT NULL,
    acao          VARCHAR(50)  NOT NULL,
    descricao     TEXT,
    data_hora     DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_utilizador) REFERENCES utilizador(id_utilizador),
    INDEX idx_data_hora (data_hora),
    INDEX idx_utilizador (id_utilizador)
);

-- ==========================
-- PEDIDOS DE ABERTURA DE CONTA
-- ==========================
CREATE TABLE IF NOT EXISTS pedido_conta (
    id_pedido   INT AUTO_INCREMENT PRIMARY KEY,
    nome        VARCHAR(150) NOT NULL,
    numero_bi   VARCHAR(30)  NOT NULL,
    telefone    VARCHAR(20),
    email       VARCHAR(100),
    tipo_conta  VARCHAR(50),
    estado      ENUM('PENDENTE','APROVADO','REJEITADO') DEFAULT 'PENDENTE',
    data_pedido DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ==========================
-- DADOS INICIAIS
-- ==========================

INSERT IGNORE INTO nivel_acesso (nome) VALUES
    ('Administrador'),
    ('Supervisor'),
    ('Atendente');

INSERT IGNORE INTO servico (nome, letra, prioritario) VALUES
    ('Caixa',                    'A', FALSE),
    ('Conta',                    'B', FALSE),
    ('Cartões',                  'C', FALSE),
    ('Atendimento Prioritário',  'D', TRUE),
    ('Outros Serviços',          'E', FALSE);

INSERT IGNORE INTO balcao (numero_balcao) VALUES (1),(2),(3),(4),(5);

-- ==========================
-- UTILIZADORES DE DEMO
-- Senhas em texto simples (migração para BCrypt via aplicação)
-- gerente/1234  →  nível Administrador
-- atendente/1234 → nível Atendente
-- ==========================
INSERT IGNORE INTO utilizador (nome, username, senha, estado, id_nivel) VALUES
    ('Gerente Demo',   'gerente',   '1234', 'ATIVO',
        (SELECT id_nivel FROM nivel_acesso WHERE nome='Administrador')),
    ('Atendente Demo', 'atendente', '1234', 'ATIVO',
        (SELECT id_nivel FROM nivel_acesso WHERE nome='Atendente'));

-- ==========================
-- VIEW ÚTIL: senhas de hoje com serviço
-- ==========================
CREATE OR REPLACE VIEW v_senhas_hoje AS
SELECT
    s.id_senha,
    s.codigo,
    sv.nome        AS servico,
    sv.letra,
    sv.prioritario,
    s.estado,
    s.numero_chamadas,
    TIME(s.data_emissao) AS hora_emissao
FROM senha s
JOIN servico sv ON s.id_servico = sv.id_servico
WHERE DATE(s.data_emissao) = CURDATE()
ORDER BY sv.prioritario DESC, s.data_emissao ASC;
