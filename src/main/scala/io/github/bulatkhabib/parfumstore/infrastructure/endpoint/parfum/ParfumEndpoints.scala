package io.github.bulatkhabib.parfumstore.infrastructure.endpoint.parfum

import cats.data.NonEmptyList
import cats.data.Validated.Valid
import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.bulatkhabib.parfumstore.domain.authentication.Auth
import io.github.bulatkhabib.parfumstore.domain.parfums.model.{Parfum, ParfumStatus}
import io.github.bulatkhabib.parfumstore.domain.parfums.service.ParfumService
import io.github.bulatkhabib.parfumstore.domain.users.model.User
import io.github.bulatkhabib.parfumstore.domain.{ParfumAlreadyExistsError, ParfumNotFoundError}
import io.github.bulatkhabib.parfumstore.infrastructure.endpoint.{AuthEndpoint, AuthService, Pagination}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes, QueryParamDecoder}
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

class ParfumEndpoints[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  import Pagination._

  implicit val statusQueryParamDecoder: QueryParamDecoder[ParfumStatus] =
    QueryParamDecoder[String].map(ParfumStatus.withName)

  object StatusMatcher extends OptionalMultiQueryParamDecoderMatcher[ParfumStatus]("status")


  implicit val parfumDecoder: EntityDecoder[F, Parfum] = jsonOf[F, Parfum]

  private def createParfumEndpoint(parfumService: ParfumService[F]): AuthEndpoint[F, Auth] = {
    case req @ POST -> Root asAuthed _ =>
      val action = for {
        create <- req.request.as[Parfum]
        parfum <- create.asParfum.pure[F]
        result <- parfumService.create(parfum).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(ParfumAlreadyExistsError(parfum)) =>
          Conflict(s"The parfum ${parfum.name} of category ${parfum.category} already exists")
      }
  }

  private def updateParfumEndpoint(parfumService: ParfumService[F]): AuthEndpoint[F, Auth] = {
    case req @ PUT -> Root asAuthed _ =>
      val action = for {
        parfum <- req.request.as[Parfum]
        result <- parfumService.update(parfum).value
      } yield  result

      action.flatMap {
        case Right(updated) => Ok(updated.asJson)
        case Left(ParfumNotFoundError) => NotFound("The parfum wasn't found")
      }
  }

  private def getParfumEndpoint(parfumService: ParfumService[F]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / LongVar(id) =>
      parfumService.get(id).value.flatMap {
        case Right(parfum) => Ok(parfum.asJson)
        case Left(ParfumNotFoundError) => NotFound("The parfum wasn't found")
      }
    }

  private def deleteParfumEndpoint(parfumService: ParfumService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root / LongVar(id) asAuthed _ =>
      for {
        _ <- parfumService.delete(id)
        resp <- Ok()
      } yield resp
  }

  private def listParfumEndpoint(parfumService: ParfumService[F]): HttpRoutes[F] =
  HttpRoutes.of[F] {
    case GET -> Root :? OptionalPageSizeMatcher(pageSize)
      :? OptionalOffsetMatcher(offset) =>
      for {
        retrieved <- parfumService.list(pageSize.getOrElse(10), offset.getOrElse(0))
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  private def findParfumsByStatusEndpoint(parfumService: ParfumService[F]): HttpRoutes[F] =
  HttpRoutes.of[F] {
    case GET -> Root / "findByStatus" :? StatusMatcher(Valid(xs)) =>
      NonEmptyList.fromList(xs) match {
        case None =>
          BadRequest("Status parameter not specified")
        case Some(status) =>
          for {
            retrieved <- parfumService.findByStatus(status)
            resp <- Ok(retrieved.asJson)
          } yield resp
      }
  }

  def endpoints(parfumService: ParfumService[F],
                auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]]): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] = {
        Auth.adminOnly(createParfumEndpoint(parfumService)
          .orElse(updateParfumEndpoint(parfumService))
          .orElse(deleteParfumEndpoint(parfumService)))
    }

    val unauthEndpoints = getParfumEndpoint(parfumService) <+> findParfumsByStatusEndpoint(parfumService) <+> listParfumEndpoint(parfumService)

    unauthEndpoints <+> auth.liftService(authEndpoints)
  }
}

object ParfumEndpoints {
  def endpoints[F[_]: Sync, Auth: JWTMacAlgo](parfumService: ParfumService[F],
                                              auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]]): HttpRoutes[F] =
    new ParfumEndpoints[F, Auth].endpoints(parfumService, auth)
}