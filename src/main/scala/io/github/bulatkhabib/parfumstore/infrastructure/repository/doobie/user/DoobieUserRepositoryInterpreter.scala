package io.github.bulatkhabib.parfumstore.infrastructure.repository.doobie.user

import cats.data.OptionT
import cats.effect.Bracket
import cats.syntax.all._
import doobie._
import doobie.implicits._
import io.github.bulatkhabib.parfumstore.domain.users.model.User
import io.github.bulatkhabib.parfumstore.domain.users.service.UserRepositoryAlgebra
import io.github.bulatkhabib.parfumstore.infrastructure.repository.doobie.SQLPagination.paginate
import tsec.authentication.IdentityStore

class DoobieUserRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends UserRepositoryAlgebra[F]
    with IdentityStore[F, Long, User] { self =>
  import UserSQL._

  def create(user: User): F[User] =
    insert(user).withUniqueGeneratedKeys[Long]("id").map(id => user.copy(id = id.some)).transact(xa)

  def update(user: User): OptionT[F, User] =
    OptionT.fromOption[F](user.id).semiflatMap { id =>
      UserSQL.update(user, id).run.transact(xa).as(user)
    }

  def get(userId: Long): OptionT[F, User] = OptionT(select(userId).option.transact(xa))

  def findByUserEmail(email: String): OptionT[F, User] =
    OptionT(byUserEmail(email).option.transact(xa))

  def delete(userId: Long): OptionT[F, User] =
    get(userId).semiflatMap(user => UserSQL.delete(userId).run.transact(xa).as(user))

  def deleteByUserEmail(email: String): OptionT[F, User] =
    findByUserEmail(email).mapFilter(_.id).flatMap(delete)

  def list(pageSize: Int, offset: Int): F[List[User]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)
}

object DoobieUserRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieUserRepositoryInterpreter[F] =
    new DoobieUserRepositoryInterpreter(xa)
}
