-- Usuários do sistema (login e permissões).
-- O e-mail é salvo normalizado (sem espaços nas pontas, em minúsculas). O CHECK garante isso
-- também para INSERTs feitos fora da aplicação (seed, script), então o UNIQUE simples basta.
CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email))),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'DRIVER'))
);
