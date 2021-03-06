package io.github.bulatkhabib.parfumstore.infrastructure.endpoint.users

import cats.effect.Sync
import cats.syntax.all._
import io.github.bulatkhabib.parfumstore.domain.users.model.User
import io.github.bulatkhabib.parfumstore.domain.users.service.UserRepositoryAlgebra
import org.http4s._
import tsec.authentication.{AugmentedJWT, IdentityStore, JWTAuthenticator, SecuredRequestHandler, buildBearerAuthHeader}
import tsec.jws.mac.{JWSMacCV, JWTMac}
import tsec.mac.jca.HMACSHA256

import scala.concurrent.duration._

class AuthTest[F[_]: Sync](userRepo: UserRepositoryAlgebra[F] with IdentityStore[F, Long, User])(
                          implicit cv: JWSMacCV[F, HMACSHA256]
) {
  private val symmetricKeyGen = HMACSHA256.unsafeGenerateKey
  private val jwtAuth: JWTAuthenticator[F, Long, User, HMACSHA256] =
    JWTAuthenticator.unbacked.inBearerToken(1.day, None, userRepo, symmetricKeyGen)

  val securedRqHandler: SecuredRequestHandler[F, Long, User, AugmentedJWT[HMACSHA256, Long]] =
    SecuredRequestHandler(jwtAuth)

  private def embedInBearerToken(r: Request[F], a: AugmentedJWT[HMACSHA256, Long]): Request[F] =
    r.putHeaders {
      val stringify = JWTMac.toEncodedString(a.jwt)
      buildBearerAuthHeader(stringify)
    }

  def embedToken(user: User, r: Request[F]): F[Request[F]] =
    for {
      u <- userRepo.create(user)
      token <- jwtAuth.create(u.id.get)
    } yield embedInBearerToken(r, token)
}
