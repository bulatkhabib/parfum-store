package io.github.bulatkhabib.parfumstore.domain.carts.service

import io.github.bulatkhabib.parfumstore.domain.carts.model.Cart

trait CartRepositoryAlgebra[F[_]] {

  def create(cart: Cart): F[Cart]

  def delete(itemId: Long, userId: Long): F[Option[Cart]]

  def get(id: Long): F[Option[Cart]]

  def list(pageSize: Int, offset: Int, userId: Long): F[List[Cart]]
}
