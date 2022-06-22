package io.github.bulatkhabib.parfumstore.domain.users.validation

import cats.Applicative
import cats.data.EitherT
import cats.syntax.all._
import io.github.bulatkhabib.parfumstore.domain.users.model.User
import io.github.bulatkhabib.parfumstore.domain.users.service.UserRepositoryAlgebra
import io.github.bulatkhabib.parfumstore.domain.{UserAlreadyExistsError, UserNotFoundError}

class UserValidationInterpreter[F[_]: Applicative](userRepo: UserRepositoryAlgebra[F]) extends UserValidationAlgebra[F] {
  override def doesNotExists(user: User): EitherT[F, UserAlreadyExistsError, Unit] =
    userRepo.findByUserEmail(user.email).map(UserAlreadyExistsError).toLeft(())

  override def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit] =
    userId match {
      case Some(value) => userRepo.get(value).toRight(UserNotFoundError).void
      case None        => EitherT.left(UserNotFoundError.pure[F])
    }
}

object UserValidationInterpreter {
  def apply[F[_]: Applicative](userRepo: UserRepositoryAlgebra[F]): UserValidationAlgebra[F] =
    new UserValidationInterpreter[F](userRepo)
}

