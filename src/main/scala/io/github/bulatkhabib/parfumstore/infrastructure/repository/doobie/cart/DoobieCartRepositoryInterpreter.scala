package io.github.bulatkhabib.parfumstore.infrastructure.repository.doobie.cart

import cats.data.OptionT
import cats.effect.Bracket
import cats.implicits.catsSyntaxOptionId
import cats.syntax.all._
import doobie._
import doobie.implicits._
import io.github.bulatkhabib.parfumstore.domain.carts.model.Cart
import io.github.bulatkhabib.parfumstore.domain.carts.service.CartRepositoryAlgebra

class DoobieCartRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends CartRepositoryAlgebra[F] {
  import CartSQL._

  def create(cart: Cart): F[Cart] =
    insert(cart).withUniqueGeneratedKeys[Long]("id").map(id => cart.copy(id = id.some)).transact(xa)

  def get(id: Long): F[Option[Cart]] = select(id).option.transact(xa)

  def list(pageSize: Int, offset: Int, userId: Long): F[List[Cart]] =
    selectAll(userId).to[List].transact(xa)

  def delete(itemId: Long, userId: Long): F[Option[Cart]] =
    OptionT(select(itemId).option)
      .semiflatMap(cart => CartSQL.delete(itemId, userId).run.as(cart))
      .value
      .transact(xa)
}

object DoobieCartRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieCartRepositoryInterpreter[F] =
    new DoobieCartRepositoryInterpreter[F](xa)
}