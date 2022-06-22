package io.github.bulatkhabib.parfumstore.domain.carts.service

import cats.data.EitherT
import cats.syntax.all._
import cats.{Functor, Monad}
import io.github.bulatkhabib.parfumstore.domain.carts.model.Cart
import io.github.bulatkhabib.parfumstore.domain.parfums.model.ParfumStatus.Unavailable
import io.github.bulatkhabib.parfumstore.domain.parfums.service.ParfumService
import io.github.bulatkhabib.parfumstore.domain.parfums.validation.ParfumValidationAlgebra
import io.github.bulatkhabib.parfumstore.domain.{CartNotFoundError, ParfumAlreadyExistsError, ParfumNotFoundError}

class CartService[F[_]](cartRepo: CartRepositoryAlgebra[F], parfumService: ParfumService[F], validationParfum: ParfumValidationAlgebra[F]) {
  def create(cart: Cart, itemId: Long)(implicit monad: Monad[F]): EitherT[F, ParfumNotFoundError.type, Cart] =
    for {
      _ <- validationParfum.exists(itemId.some)
      _ <- validationParfum.statusIsAvailable(itemId.some)
      _ <- updateParfumStatus(itemId)
      saved <- EitherT.liftF[F, ParfumNotFoundError.type, Cart](cartRepo.create(cart))
    } yield saved

  private def updateParfumStatus(itemId: Long)(implicit monad: Monad[F]): EitherT[F, ParfumNotFoundError.type, Unit] =
    EitherT {
      for {
        parfum <- parfumService.get(itemId).value.flatMap {
          case Right(parfum) => parfum.copy(status = Unavailable).pure[F]
        }
        _ <- parfumService.update(parfum).pure[F]
      } yield Either.right(())
    }

  def list(pageSize: Int, offset: Int, userId: Long): F[List[Cart]] =
    cartRepo.list(pageSize, offset, userId)

  def get(id: Long)(implicit functor: Functor[F]): EitherT[F, CartNotFoundError.type, Cart] =
    EitherT.fromOptionF(cartRepo.get(id), CartNotFoundError)

  def delete(itemId: Long, userId: Long)(implicit functor: Functor[F]): F[Unit] =
    cartRepo.delete(itemId, userId).as(())
}

object CartService {
  def apply[F[_]](repositoryAlgebra: CartRepositoryAlgebra[F],
                  parfumService: ParfumService[F],
                  validationParfum: ParfumValidationAlgebra[F]): CartService[F] =
    new CartService[F](repositoryAlgebra, parfumService, validationParfum)
}
