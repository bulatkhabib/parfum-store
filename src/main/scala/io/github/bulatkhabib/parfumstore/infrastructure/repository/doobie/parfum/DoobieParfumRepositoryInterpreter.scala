package io.github.bulatkhabib.parfumstore.infrastructure.repository.doobie.parfum

import cats.data._
import cats.effect.Bracket
import cats.syntax.all._
import doobie._
import doobie.implicits._
import io.github.bulatkhabib.parfumstore.domain.parfums.model.{Parfum, ParfumStatus}
import io.github.bulatkhabib.parfumstore.domain.parfums.service.ParfumRepositoryAlgebra
import io.github.bulatkhabib.parfumstore.infrastructure.repository.doobie.SQLPagination.paginate

class DoobieParfumRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends ParfumRepositoryAlgebra[F] {
  import ParfumSQL._

  def create(parfum: Parfum): F[Parfum] =
    insert(parfum).withUniqueGeneratedKeys[Long]("id").map(id => parfum.copy(id = id.some)).transact(xa)

  def update(parfum: Parfum): F[Option[Parfum]] =
    OptionT
      .fromOption[ConnectionIO](parfum.id)
      .semiflatMap(id => ParfumSQL.update(parfum, id).run.as(parfum))
      .value
      .transact(xa)

  def get(id: Long): F[Option[Parfum]] = select(id).option.transact(xa)

  def delete(id: Long): F[Option[Parfum]] =
    OptionT(select(id).option).semiflatMap(parfum => ParfumSQL.delete(id).run.as(parfum)).value.transact(xa)

  def findByNameAndCategory(name: String, category: String): F[Set[Parfum]] =
    selectByNameAndCategory(name, category).to[List].transact(xa).map(_.toSet)

  def list(pageSize: Int, offset: Int): F[List[Parfum]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)

  def findByStatus(status: NonEmptyList[ParfumStatus]): F[List[Parfum]] =
    selectByStatus(status).to[List].transact(xa)
}

object DoobieParfumRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieParfumRepositoryInterpreter[F] =
    new DoobieParfumRepositoryInterpreter(xa)
}
