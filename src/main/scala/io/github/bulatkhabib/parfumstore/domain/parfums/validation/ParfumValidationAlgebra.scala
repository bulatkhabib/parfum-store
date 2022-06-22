package io.github.bulatkhabib.parfumstore.domain.parfums.validation

import cats.data.EitherT
import io.github.bulatkhabib.parfumstore.domain.parfums.model.Parfum
import io.github.bulatkhabib.parfumstore.domain.{ParfumAlreadyExistsError, ParfumNotFoundError, ParfumStatusIsUnavailable}

trait ParfumValidationAlgebra[F[_]] {

  def doesNotExists(parfum: Parfum): EitherT[F, ParfumAlreadyExistsError, Unit]

  def exists(parfumId: Option[Long]): EitherT[F, ParfumNotFoundError.type, Unit]

  def statusIsAvailable(parfumId: Option[Long]): EitherT[F, ParfumNotFoundError.type, Unit]
}
