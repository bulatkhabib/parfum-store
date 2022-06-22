package io.github.bulatkhabib.parfumstore.infrastructure.endpoint.user

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.bulatkhabib.parfumstore.domain.authentication._
import io.github.bulatkhabib.parfumstore.domain.users.model.User
import io.github.bulatkhabib.parfumstore.domain.users.service.UserService
import io.github.bulatkhabib.parfumstore.domain.{UserAlreadyExistsError, UserAuthenticationFailedError, UserNotFoundError}
import io.github.bulatkhabib.parfumstore.infrastructure.endpoint.{AuthEndpoint, AuthService, Pagination}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import tsec.authentication._
import tsec.common.Verified
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.passwordhashers.{PasswordHash, PasswordHasher}

class UserEndpoints[F[_]: Sync, A, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  import Pagination._

  implicit val userDecoder: EntityDecoder[F, User] = jsonOf
  implicit val loginReqDecoder: EntityDecoder[F, LoginRequest] = jsonOf

  implicit val signupReqDecoder: EntityDecoder[F, SignupRequest] = jsonOf

  private def loginEndpoint(
                             userService: UserService[F],
                             cryptService: PasswordHasher[F, A],
                             auth: Authenticator[F, Long, User, AugmentedJWT[Auth, Long]],
                           ): HttpRoutes[F] =
    HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
      val action = for {
        login <- EitherT.liftF(req.as[LoginRequest])
        email = login.email
        user <- userService.getUserByEmail(email).leftMap(_ => UserAuthenticationFailedError(email))
        checkResult <- EitherT.liftF(cryptService.checkpw(login.password, PasswordHash[A](user.hash)))
        _ <-
          if (checkResult == Verified) EitherT.rightT[F, UserAuthenticationFailedError](())
          else EitherT.leftT[F, User](UserAuthenticationFailedError(email))
        token <- user.id match {
          case Some(value) => EitherT.right[UserAuthenticationFailedError](auth.create(value))
          case None => throw new Exception("Incorrect")
        }
      } yield (user, token)

      action.value.flatMap {
        case Right((user, token)) => Ok(user.toUserWithoutHash.asJson).map(auth.embed(_, token))
        case Left(UserAuthenticationFailedError(email)) => BadRequest(s"Authentication failed for user $email")
      }
    }

  private def signupEndpoint(
                            userService: UserService[F],
                            crypt: PasswordHasher[F, A]
                            ): HttpRoutes[F] =
    HttpRoutes.of[F] { case req @ POST -> Root / "signup" =>
      val action = for {
        signup <- req.as[SignupRequest]
        hash <- crypt.hashpw(signup.password)
        user <- signup.asUser(hash).pure[F]
        result <- userService.createUser(user).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.toUserWithoutHash.asJson)
        case Left(UserAlreadyExistsError(user)) =>
          Conflict(s"The user with email ${user.email} already exists")
      }
    }

  private def updateEndpoint(userService: UserService[F]): AuthEndpoint[F, Auth] = {
    case req @ PUT -> Root / email asAuthed _ =>
      val action = for {
        user <- req.request.as[User]
        updated = user.copy(email = email)
        result <- userService.updateUser(updated).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(UserNotFoundError) => NotFound("User not found")
      }
  }

  private def listEndpoint(userService: UserService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(
        offset
      ) asAuthed _ =>
        for {
          retrieved <- userService.list(pageSize.getOrElse(10), offset.getOrElse(0))
          response <- Ok(retrieved.asJson)
        } yield response
  }

  private def searchByEmailEndpoint(userService: UserService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root / email asAuthed _ =>
      userService.getUserByEmail(email).value.flatMap {
        case Right(found) => Ok(found.asJson)
        case Left(UserNotFoundError) => NotFound("User wasn't found")
      }
  }

  private def deleteUserEndpoint(userService: UserService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root / email asAuthed _ =>
      for {
        _ <- userService.deleteUserByEmail(email)
        response <- Ok()
      } yield response
  }

  def endpoints(
               userService: UserService[F],
               cryptService: PasswordHasher[F, A],
               auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]]
               ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] =
      Auth.adminOnly {
        updateEndpoint(userService)
          .orElse(listEndpoint(userService))
          .orElse(searchByEmailEndpoint(userService))
          .orElse(deleteUserEndpoint(userService))
      }

    val unauthEndpoints =
      loginEndpoint(userService, cryptService, auth.authenticator) <+>
        signupEndpoint(userService, cryptService)

    unauthEndpoints <+> auth.liftService(authEndpoints)
  }
}

object UserEndpoints {
  def endpoints[F[_]: Sync, A, Auth: JWTMacAlgo](
                                                userService: UserService[F],
                                                cryptService: PasswordHasher[F, A],
                                                auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]]
                                                ): HttpRoutes[F] =
    new UserEndpoints[F, A, Auth].endpoints(userService, cryptService, auth)
}
