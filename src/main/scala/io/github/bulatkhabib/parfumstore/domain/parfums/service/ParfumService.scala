package io.github.bulatkhabib.parfumstore.domain.parfums.service

import cats.data.{EitherT, NonEmptyList}
import cats.syntax.all._
import cats.{Functor, Monad}
import io.github.bulatkhabib.parfumstore.domain.parfums.model.{Parfum, ParfumStatus}
import io.github.bulatkhabib.parfumstore.domain.parfums.validation.ParfumValidationAlgebra
import io.github.bulatkhabib.parfumstore.domain.{ParfumAlreadyExistsError, ParfumNotFoundError}

class ParfumService[F[_]](parfumRepo: ParfumRepositoryAlgebra[F], validation: ParfumValidationAlgebra[F]) {
  def create(parfum: Parfum)(implicit monad: Monad[F]): EitherT[F, ParfumAlreadyExistsError, Parfum] =
    for {
      _ <- validation.doesNotExists(parfum)
      saved <- EitherT.liftF(parfumRepo.create(parfum))
    } yield saved

  def update(parfum: Parfum)(implicit monad: Monad[F]): EitherT[F, ParfumNotFoundError.type, Parfum] =
    for {
      _ <- validation.exists(parfum.id)
      updated <- EitherT.fromOptionF(parfumRepo.update(parfum), ParfumNotFoundError)
    } yield updated

  def get(id: Long)(implicit functor: Functor[F]): EitherT[F, ParfumNotFoundError.type, Parfum] =
    EitherT.fromOptionF(parfumRepo.get(id), ParfumNotFoundError)

  def delete(id: Long)(implicit functor: Functor[F]): F[Unit] =
    parfumRepo.delete(id).as(())

  def list(pageSize: Int, offset: Int): F[List[Parfum]] =
    parfumRepo.list(pageSize, offset)

  def findByStatus(status: NonEmptyList[ParfumStatus]): F[List[Parfum]] =
    parfumRepo.findByStatus(status)
}

object ParfumService {
  def apply[F[_]](repositoryAlgebra: ParfumRepositoryAlgebra[F],
                  validationAlgebra: ParfumValidationAlgebra[F]): ParfumService[F] =
    new ParfumService[F](repositoryAlgebra, validationAlgebra)
}
