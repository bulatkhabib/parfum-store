package io.github.bulatkhabib.parfumstore.infrastructure.repository.inmemory

import cats.Applicative
import cats.data.OptionT
import cats.syntax.all._
import io.github.bulatkhabib.parfumstore.domain.users.model.User
import io.github.bulatkhabib.parfumstore.domain.users.service.UserRepositoryAlgebra
import tsec.authentication.IdentityStore

import java.util.Random
import scala.collection.concurrent.TrieMap

class UserRepositoryInMemoryInterpreter[F[_]: Applicative]
    extends UserRepositoryAlgebra[F]
    with IdentityStore[F, Long, User] {

  private val cache = new TrieMap[Long, User]
  private val random = new Random

  override def create(user: User): F[User] = {
    val id = random.nextLong()
    val toSave = user.copy(id = id.some)
    cache += (id -> toSave)
    toSave.pure[F]
  }

  override def update(user: User): OptionT[F, User] =
    OptionT {
      user.id.traverse { id =>
        cache.update(id, user)
        user.pure[F]
      }
    }

  override def delete(userId: Long): OptionT[F, User] =
    OptionT.fromOption(cache.remove(userId))

  override def findByUserEmail(email: String): OptionT[F, User] =
    OptionT.fromOption(cache.values.find(user => user.email == email))

  override def deleteByUserEmail(email: String): OptionT[F, User] =
    OptionT.fromOption(
      for {
        user <- cache.values.find(user => user.email == email)
        removed <- cache.remove(user.id.get)
      } yield removed
    )

  override def list(pageSize: Int, offset: Int): F[List[User]] =
    cache.values
      .toList
      .sortBy(_.lastName)
      .slice(offset, offset + pageSize)
      .pure[F]

  override def get(id: Long): OptionT[F, User] =
    OptionT.fromOption(cache.get(id))
}

object UserRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new UserRepositoryInMemoryInterpreter[F]
}