package io.github.bulatkhabib.parfumstore.domain.users.service

import cats.data.OptionT
import io.github.bulatkhabib.parfumstore.domain.users.model.User

trait UserRepositoryAlgebra[F[_]] {
  def create(user: User): F[User]

  def update(user: User): OptionT[F, User]

  def get(userId: Long): OptionT[F, User]

  def delete(userId: Long): OptionT[F, User]

  def findByUserEmail(email: String): OptionT[F, User]

  def deleteByUserEmail(email: String): OptionT[F, User]

  def list(pageSize: Int, offset: Int): F[List[User]]
}
