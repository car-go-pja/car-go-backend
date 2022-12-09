package com.cargo.model

import java.time.Instant
import java.util.UUID

final case class VerificationRow(id: UUID, userId: UUID, code: String, createdAt: Instant)
