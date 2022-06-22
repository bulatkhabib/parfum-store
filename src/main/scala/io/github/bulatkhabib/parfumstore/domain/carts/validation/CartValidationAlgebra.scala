package io.github.bulatkhabib.parfumstore.domain.carts.validation

import cats.data.EitherT
import io.github.bulatkhabib.parfumstore.domain.CartNotFoundError

trait CartValidationAlgebra[F[_]] {

  def exists(cartId: Option[Long]): EitherT[F, CartNotFoundError.type, Unit]
}
