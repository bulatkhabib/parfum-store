package io.github.bulatkhabib.parfumstore.infrastructure.endpoint.parfums

import cats.data.NonEmptyList
import cats.effect.IO
import io.github.bulatkhabib.parfumstore.Arbitraries
import io.github.bulatkhabib.parfumstore.domain.parfums.model.{Parfum, ParfumStatus}
import org.http4s.{EntityDecoder, EntityEncoder, HttpApp, Uri}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import io.circe.generic.auto._
import io.github.bulatkhabib.parfumstore.domain.parfums.service.ParfumService
import io.github.bulatkhabib.parfumstore.domain.parfums.validation.ParfumValidationInterpreter
import io.github.bulatkhabib.parfumstore.infrastructure.endpoint.parfum.ParfumEndpoints
import io.github.bulatkhabib.parfumstore.infrastructure.endpoint.users.AuthTest
import io.github.bulatkhabib.parfumstore.infrastructure.repository.inmemory.{ParfumRepositoryInMemoryInterpreter, UserRepositoryInMemoryInterpreter}
import org.http4s.server.Router
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tsec.mac.jca.HMACSHA256

class ParfumEndpointsSpec
    extends AnyFunSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with Arbitraries
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {

  implicit val parfumEncoder: EntityEncoder[IO, Parfum] = jsonEncoderOf
  implicit val parfumDecoder: EntityDecoder[IO, Parfum] = jsonOf

  def getTestResources(): (AuthTest[IO], HttpApp[IO], ParfumRepositoryInMemoryInterpreter[IO]) = {
    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
    val parfumRepo = ParfumRepositoryInMemoryInterpreter[IO]()
    val parfumValidation = ParfumValidationInterpreter[IO](parfumRepo)
    val parfumService = ParfumService[IO](parfumRepo, parfumValidation)
    val auth = new AuthTest[IO](userRepo)
    val parfumEndpoint = ParfumEndpoints.endpoints[IO, HMACSHA256](parfumService, auth.securedRqHandler)
    val parfumRoutes = Router(("/parfums", parfumEndpoint)).orNotFound
    (auth, parfumRoutes, parfumRepo)
  }

  test("find by status") {
    val (auth, parfumRoutes, parfumRepo) = getTestResources()

    forAll { (parfum: Parfum, user: AdminUser) =>
      (for {
        createReq <- POST(parfum, uri"/parfums")
          .flatMap(auth.embedToken(user.value, _))
        createResp <- parfumRoutes.run(createReq)
        createdParfum <- createResp.as[Parfum]
      } yield {
        val parfumsByStatus = parfumRepo.findByStatus(NonEmptyList.of(createdParfum.status)).unsafeRunSync()
        parfumsByStatus.contains(createdParfum)
      }).unsafeRunSync()
    }
  }

  test("update parfum") {
    val (auth, parfumRoutes, _) = getTestResources()

    forAll { (parfum: Parfum, user: AdminUser) =>
      (for {
        createReq <- POST(parfum, uri"/parfums")
          .flatMap(auth.embedToken(user.value, _))
        createResp <- parfumRoutes.run(createReq)
        createdParfum <- createResp.as[Parfum]
        parfumToUpdate = createdParfum.copy(name = createdParfum.name.reverse)
        updateReq <- PUT(parfumToUpdate, Uri.unsafeFromString(s"/parfums"))
          .flatMap(auth.embedToken(user.value, _))
        updateResp <- parfumRoutes.run(updateReq)
        updatedParfum <- updateResp.as[Parfum]
      } yield updatedParfum.name shouldEqual parfum.name.reverse).unsafeRunSync()
    }
  }
}
