package io.github.bulatkhabib.parfumstore.domain.parfums.validation

import cats.Applicative
import cats.data.EitherT
import cats.syntax.all._
import io.github.bulatkhabib.parfumstore.domain.parfums.model.Parfum
import io.github.bulatkhabib.parfumstore.domain.parfums.model.ParfumStatus.Available
import io.github.bulatkhabib.parfumstore.domain.parfums.service.ParfumRepositoryAlgebra
import io.github.bulatkhabib.parfumstore.domain.{ParfumAlreadyExistsError, ParfumNotFoundError, ParfumStatusIsUnavailable}

case class ParfumValidationInterpreter[F[_] : Applicative](repositoryAlgebra: ParfumRepositoryAlgebra[F])
  extends ParfumValidationAlgebra[F] {

  override def doesNotExists(parfum: Parfum): EitherT[F, ParfumAlreadyExistsError, Unit] =
    EitherT {
      repositoryAlgebra.findByNameAndCategory(parfum.name, parfum.category).map { matches =>
        if (matches.forall(p => p.category != parfum.category)) {
          Right(())
        } else {
          Left(ParfumAlreadyExistsError(parfum))
        }
      }
    }

  override def exists(parfumId: Option[Long]): EitherT[F, ParfumNotFoundError.type, Unit] =
    EitherT {
      parfumId match {
        case Some(value) => repositoryAlgebra.get(value).map {
          case Some(_) => Right(())
          case None => Left(ParfumNotFoundError)
        }
        case _ => Either.left[ParfumNotFoundError.type, Unit](ParfumNotFoundError).pure[F]
      }
    }

  override def statusIsAvailable(parfumId: Option[Long]): EitherT[F, ParfumNotFoundError.type, Unit] =
    EitherT {
      parfumId match {
        case Some(value) => repositoryAlgebra.get(value).map {
          case Some(parfum) => if (parfum.status == Available) Right(()) else Left(ParfumNotFoundError)
          case None => Left(ParfumNotFoundError)
        }
        case _ => Either.left[ParfumNotFoundError.type, Unit](ParfumNotFoundError).pure[F]
      }
    }
}
