package com.cargo.model

import java.util.UUID

final case class User(id: UUID, email: String, password: String) //change to secret
