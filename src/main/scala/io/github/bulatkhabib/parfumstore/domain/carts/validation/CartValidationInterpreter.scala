package io.github.bulatkhabib.parfumstore.domain.carts.validation

import cats.Applicative
import cats.syntax.all._
import cats.data.EitherT
import io.github.bulatkhabib.parfumstore.domain.CartNotFoundError
import io.github.bulatkhabib.parfumstore.domain.carts.service.CartRepositoryAlgebra

case class CartValidationInterpreter[F[_] : Applicative](repositoryAlgebra: CartRepositoryAlgebra[F])
  extends CartValidationAlgebra[F] {

  override def exists(cartId: Option[Long]): EitherT[F, CartNotFoundError.type, Unit] =
    EitherT {
      cartId match {
        case Some(value) => repositoryAlgebra.get(value).map {
          case Some(_) => Right(())
          case None => Left(CartNotFoundError)
        }
        case None => Either.left[CartNotFoundError.type, Unit](CartNotFoundError).pure[F]
      }
    }
}
