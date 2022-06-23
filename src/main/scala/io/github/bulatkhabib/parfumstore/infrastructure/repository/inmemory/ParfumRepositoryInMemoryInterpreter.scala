package io.github.bulatkhabib.parfumstore.infrastructure.repository.inmemory

import cats.Applicative
import cats.syntax.all._
import cats.data.NonEmptyList
import io.github.bulatkhabib.parfumstore.domain.parfums.model.{Parfum, ParfumStatus}
import io.github.bulatkhabib.parfumstore.domain.parfums.service.ParfumRepositoryAlgebra

import scala.collection.concurrent.TrieMap
import scala.util.Random

class ParfumRepositoryInMemoryInterpreter[F[_]: Applicative] extends ParfumRepositoryAlgebra[F] {

  private val cache = new TrieMap[Long, Parfum]

  private val random = new Random

  override def create(parfum: Parfum): F[Parfum] = {
    val id = random.nextLong()
    val toSave = parfum.copy(id = id.some)
    cache += (id -> parfum.copy(id = id.some))
    toSave.pure[F]
  }

  override def update(parfum: Parfum): F[Option[Parfum]] =
    parfum.id.traverse { id =>
      cache.update(id, parfum)
      parfum.pure[F]
    }

  override def delete(id: Long): F[Option[Parfum]] =
    cache.remove(id).pure[F]

  override def get(id: Long): F[Option[Parfum]] =
    cache.get(id).pure[F]

  override def findByNameAndCategory(name: String, category: String): F[Set[Parfum]] =
    cache.values
      .filter(parfum => parfum.name == name && parfum.category == category)
      .toSet
      .pure[F]

  override def findByStatus(status: NonEmptyList[ParfumStatus]): F[List[Parfum]] =
    cache.values
      .filter(parfum => status.exists(_ == parfum.status))
      .toList
      .pure[F]

  override def list(pageSize: Int, offset: Int): F[List[Parfum]] =
    cache.values
      .toList
      .sortBy(_.name)
      .slice(offset, offset + pageSize)
      .pure[F]
}

object ParfumRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new ParfumRepositoryInMemoryInterpreter[F]()
}
