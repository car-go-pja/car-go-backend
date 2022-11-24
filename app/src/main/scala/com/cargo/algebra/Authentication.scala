package com.cargo.algebra

import zio._

trait Authentication {
  def register(email: String, password: String): Task[Unit]
}

object Authentication {}
