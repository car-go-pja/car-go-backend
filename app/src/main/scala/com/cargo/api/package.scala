package com.cargo

package object api {
  def parseToken(bearerToken: String): String = bearerToken.drop(7)
}
