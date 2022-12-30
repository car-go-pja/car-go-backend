package com.cargo.infrastructure

import doobie._
import doobie.util.Put
import io.estatico.newtype._
import io.estatico.newtype.ops._

trait DoobieInstances extends postgres.Instances {

  implicit def newTypePut[N: Coercible[*, R], R: Put]: Put[N] = Put[R].contramap[N](_.coerce[R])
  implicit def newTypeRead[N: Coercible[R, *], R: Read]: Read[N] = Read[R].map(_.asInstanceOf[N])
}
