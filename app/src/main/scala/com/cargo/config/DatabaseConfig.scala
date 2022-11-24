package com.cargo.config

final case class DatabaseConfig(
    jdbcUrl: String,
    username: String,
    password: String
)
