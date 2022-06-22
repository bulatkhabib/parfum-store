package io.github.bulatkhabib.parfumstore.domain.users.service

import cats.data.EitherT
import cats.syntax.functor._
import cats.{Functor, Monad}
import io.github.bulatkhabib.parfumstore.domain.users.model.User
import io.github.bulatkhabib.parfumstore.domain.users.validation.UserValidationAlgebra
import io.github.bulatkhabib.parfumstore.domain.{UserAlreadyExistsError, UserNotFoundError}

class UserService[F[_]](userRepo: UserRepositoryAlgebra[F], validation: UserValidationAlgebra[F]) {
  def createUser(user: User)(implicit monad: Monad[F]): EitherT[F, UserAlreadyExistsError, User] = {
    for {
      _ <- validation.doesNotExists(user)
      saved <- EitherT.liftF(userRepo.create(user))
    } yield saved
  }

  def getUser(userId: Long)(implicit functor: Functor[F]): EitherT[F, UserNotFoundError.type, User] =
    userRepo.get(userId).toRight(UserNotFoundError)

  def getUserByEmail(email: String)(implicit functor: Functor[F]): EitherT[F, UserNotFoundError.type, User] =
    userRepo.findByUserEmail(email).toRight(UserNotFoundError)

  def updateUser(user: User)(implicit monad: Monad[F]): EitherT[F, UserNotFoundError.type, User] =
    for {
      _ <- validation.exists(user.id)
      updated <- userRepo.update(user).toRight(UserNotFoundError)
    } yield updated

  def deleteUser(userId: Long)(implicit functor: Functor[F]): F[Unit] =
    userRepo.delete(userId).value.void

  def deleteUserByEmail(email: String)(implicit functor: Functor[F]): F[Unit] =
    userRepo.deleteByUserEmail(email).value.void

  def list(pageSize: Int, offset: Int): F[List[User]] =
    userRepo.list(pageSize, offset)
}

object UserService {
  def apply[F[_]](repository: UserRepositoryAlgebra[F], validation: UserValidationAlgebra[F]): UserService[F] =
    new UserService[F](repository, validation)
}
