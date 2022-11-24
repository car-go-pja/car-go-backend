CREATE SCHEMA IF NOT EXISTS cargo;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS cargo.users
(
    id             UUID                     DEFAULT gen_random_uuid()        NOT NULL PRIMARY KEY,
    email          VARCHAR(32)                                               NOT NULL UNIQUE,
    password       VARCHAR(128)                                              NOT NULL
);