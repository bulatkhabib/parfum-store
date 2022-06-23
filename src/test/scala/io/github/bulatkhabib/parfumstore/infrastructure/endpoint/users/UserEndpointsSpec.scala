package io.github.bulatkhabib.parfumstore
package infrastructure
package endpoint
package users

import io.github.bulatkhabib.parfumstore.Arbitraries
import io.github.bulatkhabib.parfumstore.domain.users.service.UserService
import io.github.bulatkhabib.parfumstore.domain.users.model.User
import io.github.bulatkhabib.parfumstore.domain.authentication.SignupRequest
import io.github.bulatkhabib.parfumstore.infrastructure.endpoint.user.UserEndpoints
import infrastructure.repository.inmemory.UserRepositoryInMemoryInterpreter
import io.github.bulatkhabib.parfumstore.domain.users.validation.UserValidationInterpreter
import cats.effect._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl._
import tsec.passwordhashers.jca.BCrypt
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.server.Router
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tsec.authentication.{JWTAuthenticator, SecuredRequestHandler}
import tsec.mac.jca.HMACSHA256
import org.scalatest.matchers.should.Matchers
import tsec.passwordhashers.PasswordHasher

import scala.concurrent.duration.DurationInt

class UserEndpointsSpec
  extends AnyFunSuite
  with Matchers
  with ScalaCheckPropertyChecks
  with Arbitraries
  with Http4sDsl[IO]
  with Http4sClientDsl[IO]
  with LoginTest {

  def userRoutes(): HttpApp[IO] = {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val userValidation = UserValidationInterpreter[IO](userRepo)
    val userService = UserService[IO](userRepo, userValidation)
    val key = HMACSHA256.unsafeGenerateKey
    val jwtAuth = JWTAuthenticator.unbacked.inBearerToken(1.day, None, userRepo, key)
    val usersEndpoints = UserEndpoints.endpoints(
      userService,
      BCrypt.syncPasswordHasher[IO],
      SecuredRequestHandler(jwtAuth)
    )
    Router(("/users", usersEndpoints)).orNotFound
  }

  test("create user and log in") {
    val userEndpoint = userRoutes()

    forAll { userSignup: SignupRequest =>
      val (_, authorization) = signUpAndLogin(userSignup, userEndpoint).unsafeRunSync()
      authorization shouldBe defined
    }
  }

  test("get user by email") {
    val userEndpoint = userRoutes()

    forAll { userSignup: SignupRequest =>
      (for {
        loginResp <- signUpAndLogin(userSignup, userEndpoint)
        (createdUser, authorization) = loginResp
        getRequest <- GET(Uri.unsafeFromString(s"/users/${createdUser.email}"))
        getRequestAuth = getRequest.putHeaders(authorization.get)
        getResponse <- userEndpoint.run(getRequestAuth)
        getUser <- getResponse.as[User]
      } yield {
        getResponse.status shouldEqual Ok
        createdUser.email shouldEqual getUser.email
      }).unsafeRunSync()
    }
  }
}
