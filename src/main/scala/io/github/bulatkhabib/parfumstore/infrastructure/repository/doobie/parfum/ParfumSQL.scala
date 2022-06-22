package io.github.bulatkhabib.parfumstore.infrastructure.repository.doobie.parfum

import cats.data._
import doobie._
import doobie.implicits._
import io.github.bulatkhabib.parfumstore.domain.parfums.model.{Parfum, ParfumStatus}

private object ParfumSQL {

  implicit val StatusMeta: Meta[ParfumStatus] =
    Meta[String].imap(ParfumStatus.withName)(_.entryName)

  def insert(parfum: Parfum): Update0 = sql"""
    INSERT INTO PARFUM (NAME, CATEGORY, DESCRIPTION, PRICE, STATUS)
    VALUES (${parfum.name}, ${parfum.category}, ${parfum.description}, ${parfum.price}, ${parfum.status})
  """.update

  def update(parfum: Parfum, id: Long): Update0 = sql"""
    UPDATE PARFUM
    SET NAME = ${parfum.name}, CATEGORY = ${parfum.category}, DESCRIPTION = ${parfum.description}, PRICE = ${parfum.price}, STATUS = ${parfum.status}
    WHERE ID = $id
  """.update

  def select(id: Long): Query0[Parfum] = sql"""
    SELECT NAME, CATEGORY, DESCRIPTION, PRICE, STATUS, ID
    FROM PARFUM
    WHERE ID = $id
  """.query

  def delete(id: Long): Update0 = sql"""
    DELETE FROM PARFUM WHERE ID = $id
  """.update

  def selectByNameAndCategory(name: String, category: String): Query0[Parfum] = sql"""
    SELECT NAME, CATEGORY, DESCRIPTION, PRICE, STATUS, ID
    FROM PARFUM
    WHERE NAME = $name AND CATEGORY = $category
  """.query[Parfum]

  def selectByStatus(status: NonEmptyList[ParfumStatus]): Query0[Parfum] =
    (
      sql"""
    SELECT NAME, CATEGORY, DESCRIPTION, PRICE, STATUS, ID
    FROM PARFUM
    WHERE """ ++ Fragments.in(fr"STATUS", status)
      ).query


  def selectAll: Query0[Parfum] = sql"""
    SELECT NAME, CATEGORY, DESCRIPTION, PRICE, STATUS, ID
    FROM PARFUM
    ORDER BY NAME
  """.query
}
