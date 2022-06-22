package io.github.bulatkhabib.parfumstore.domain.users.validation

import cats.data.EitherT
import io.github.bulatkhabib.parfumstore.domain.users.model.User
import io.github.bulatkhabib.parfumstore.domain.{UserAlreadyExistsError, UserNotFoundError}

trait UserValidationAlgebra[F[_]] {
  def doesNotExists(user: User): EitherT[F, UserAlreadyExistsError, Unit]

  def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit]
}
