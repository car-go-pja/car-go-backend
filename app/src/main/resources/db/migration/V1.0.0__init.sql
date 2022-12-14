CREATE SCHEMA IF NOT EXISTS cargo;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS cargo.users
(
    id             UUID                     DEFAULT gen_random_uuid()        NOT NULL PRIMARY KEY,
    email          VARCHAR(32)                                               NOT NULL UNIQUE,
    password       VARCHAR(128)                                              NOT NULL,
    is_verified    BOOLEAN                                                   DEFAULT false
);

CREATE TABLE IF NOT EXISTS cargo.verification
(
    id             UUID                     DEFAULT gen_random_uuid()        NOT NULL PRIMARY KEY,
    user_id        UUID                                                      NOT NULL REFERENCES cargo.users(id),
    code           VARCHAR(6)                                                NOT NULL,
    created_at     TIMESTAMPTZ                                               NOT NULL
);