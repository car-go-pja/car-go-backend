package com.cargo.algebra

import com.cargo.model.AccessToken
import zio._

trait Tokens {
  def issue: Task[AccessToken]
  def verify: IO[Throwable, Unit]
}

object Tokens
