package com.cargo.model

import java.util.UUID

final case class UserInfo(id: UUID, email: String, isVerified: Boolean)
