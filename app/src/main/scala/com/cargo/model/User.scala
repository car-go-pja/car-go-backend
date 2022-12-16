package com.cargo.model

import java.util.UUID

final case class User(
    id: UUID,
    email: String,
    password: String,
    isVerified: Boolean
) //change to secret
