package io.github.bulatkhabib.parfumstore.infrastructure.endpoint.cart

import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import io.github.bulatkhabib.parfumstore.domain.ParfumNotFoundError
import io.github.bulatkhabib.parfumstore.domain.authentication.Auth
import io.github.bulatkhabib.parfumstore.domain.carts.model.Cart
import io.github.bulatkhabib.parfumstore.domain.carts.service.CartService
import io.github.bulatkhabib.parfumstore.domain.parfums.service.ParfumService
import io.github.bulatkhabib.parfumstore.domain.users.model.User
import io.github.bulatkhabib.parfumstore.infrastructure.endpoint.Pagination.{OptionalOffsetMatcher, OptionalPageSizeMatcher}
import io.github.bulatkhabib.parfumstore.infrastructure.endpoint.{AuthEndpoint, AuthService}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

class CartEndpoints[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {

  implicit val cartDecoder: EntityDecoder[F, Cart] = jsonOf[F, Cart]

  private def createCartEndpoint(cartService: CartService[F]): AuthEndpoint[F, Auth] = {
    case  POST -> Root / LongVar(id) asAuthed user =>
      val action = for {
        cart <- Cart(user.id.get, id).pure[F]
        result <- cartService.create(cart, id).value
      } yield result

      action.flatMap {
        case Right(saved) =>Ok(saved.asJson)
        case Left(ParfumNotFoundError) => NotFound("The parfum wasn't found")
      }
  }

  def deleteItemEndpoint(cartService: CartService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root / LongVar(id) asAuthed user =>
      for {
        _ <- cartService.delete(id, user.id.get)
        resp <- Ok()
      } yield resp
  }

  def listCartEndpoint(cartService: CartService[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root :? OptionalPageSizeMatcher(pageSize)
      :? OptionalOffsetMatcher(offset) asAuthed user =>
      for {
        retrieved <- cartService.list(pageSize.getOrElse(10), offset.getOrElse(0), user.id.get)
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  def endpoints(cartService: CartService[F],
                auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]]): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] = {
      val allRoles =
        createCartEndpoint(cartService)
          .orElse(listCartEndpoint(cartService))
          .orElse(deleteItemEndpoint(cartService))


      Auth.allRolesHandler(allRoles)(Auth.adminOnly(allRoles))
    }

    auth.liftService(authEndpoints)
  }
}

object CartEndpoints {
  def endpoints[F[_]: Sync, Auth: JWTMacAlgo](cartService: CartService[F],
                                              auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]]): HttpRoutes[F] =
    new CartEndpoints[F, Auth].endpoints(cartService, auth)
}