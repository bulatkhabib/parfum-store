package io.github.bulatkhabib.parfumstore.infrastructure.endpoint.users

import cats.effect.IO
import io.github.bulatkhabib.parfumstore.domain.authentication.{LoginRequest, SignupRequest}
import io.github.bulatkhabib.parfumstore.domain.users.model.User
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.implicits._
import io.circe.generic.auto._
import org.http4s.{EntityDecoder, EntityEncoder, HttpApp}
import tsec.passwordhashers.PasswordHasher

trait LoginTest extends Http4sClientDsl[IO] with Http4sDsl[IO] {

  implicit val userEnc: EntityEncoder[IO, User] = jsonEncoderOf
  implicit val userDec: EntityDecoder[IO, User] = jsonOf

  implicit val signUpRequestEnc: EntityEncoder[IO, SignupRequest] = jsonEncoderOf
  implicit val signUpRequestDec: EntityDecoder[IO, SignupRequest] = jsonOf

  implicit val loginRequestEnc: EntityEncoder[IO, LoginRequest] = jsonEncoderOf
  implicit val loginRequestDec: EntityDecoder[IO, LoginRequest] = jsonOf

  def signUpAndLogin(userSignUp: SignupRequest,
                     userEndpoint: HttpApp[IO]): IO[(User, Option[Authorization])] =
    for {
//      hash <- crypt.hashpw(userSignUp.password)
      signUpReq <- POST(userSignUp, uri"/users")
      signUpResp <- userEndpoint.run(signUpReq)
      user <- signUpResp.as[User]
      loginBody = LoginRequest(userSignUp.email, userSignUp.password)
      loginReq <- POST(loginBody, uri"/users/login")
      loginResp <- userEndpoint.run(loginReq)
    } yield user -> loginResp.headers.get(Authorization)
}
