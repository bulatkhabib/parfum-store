package io.github.bulatkhabib.parfumstore.infrastructure.repository.doobie.auth

import cats._
import cats.data._
import cats.effect.Bracket
import cats.syntax.all._
import doobie._
import doobie.implicits._
import tsec.authentication.{AugmentedJWT, BackingStore}
import tsec.common.SecureRandomId
import tsec.jws.JWSSerializer
import tsec.jws.mac.{JWSMacCV, JWSMacHeader, JWTMacImpure}
import tsec.mac.jca.{MacErrorM, MacSigningKey}

import java.time.Instant

class DoobieAuthRepositoryInterpreter[F[_]: Bracket[*[_], Throwable], A](
                                                                          val key: MacSigningKey[A],
                                                                          val xa: Transactor[F],
                                                                        )(implicit
                                                                          hs: JWSSerializer[JWSMacHeader[A]],
                                                                          s: JWSMacCV[MacErrorM, A],
                                                                        ) extends BackingStore[F, SecureRandomId, AugmentedJWT[A, Long]] {
  override def put(jwt: AugmentedJWT[A, Long]): F[AugmentedJWT[A, Long]] =
    AuthSQL.insert(jwt).run.transact(xa).as(jwt)

  override def update(jwt: AugmentedJWT[A, Long]): F[AugmentedJWT[A, Long]] =
    AuthSQL.update(jwt).run.transact(xa).as(jwt)

  override def delete(id: SecureRandomId): F[Unit] =
    AuthSQL.delete(id).run.transact(xa).void

  override def get(id: SecureRandomId): OptionT[F, AugmentedJWT[A, Long]] =
    OptionT(AuthSQL.select(id).option.transact(xa)).semiflatMap {
      case (jwtStringify, identity, expiry, lastTouched) =>
        JWTMacImpure.verifyAndParse(jwtStringify, key) match {
          case Left(err) => err.raiseError[F, AugmentedJWT[A, Long]]
          case Right(jwt) => AugmentedJWT(id, jwt, identity, expiry, lastTouched).pure[F]
        }
    }
}

object DoobieAuthRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable], A](key: MacSigningKey[A], xa: Transactor[F])(implicit
                                                                                         hs: JWSSerializer[JWSMacHeader[A]],
                                                                                         s: JWSMacCV[MacErrorM, A],
  ): DoobieAuthRepositoryInterpreter[F, A] =
    new DoobieAuthRepositoryInterpreter(key, xa)
}
