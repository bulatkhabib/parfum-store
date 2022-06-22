package io.github.bulatkhabib.parfumstore.domain.parfums.service

import cats.data.NonEmptyList
import io.github.bulatkhabib.parfumstore.domain.parfums.model.{Parfum, ParfumStatus}

trait ParfumRepositoryAlgebra[F[_]] {

  def create(parfum: Parfum): F[Parfum]

  def update(parfum: Parfum): F[Option[Parfum]]

  def delete(id: Long): F[Option[Parfum]]

  def get(id: Long): F[Option[Parfum]]

  def findByNameAndCategory(name: String, category: String): F[Set[Parfum]]

  def findByStatus(status: NonEmptyList[ParfumStatus]): F[List[Parfum]]

  def list(pageSize: Int, offset: Int): F[List[Parfum]]
}
