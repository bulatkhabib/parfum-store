package io.github.bulatkhabib.parfumstore.infrastructure.repository.doobie.user

import cats.syntax.all._
import doobie._
import doobie.implicits._
import io.circe.parser.decode
import io.circe.syntax._
import io.github.bulatkhabib.parfumstore.domain.users.model.{Role, User}

private object UserSQL {

  implicit val roleMeta: Meta[Role] =
    Meta[String].imap(decode[Role](_).leftMap(throw _).merge)(_.asJson.toString)

  def insert(user: User): Update0 = sql"""
    INSERT INTO USERS (FIRST_NAME, LAST_NAME, EMAIL, HASH, PHONE, ROLE)
    VALUES (${user.firstName}, ${user.lastName}, ${user.email}, ${user.hash}, ${user.phone}, ${user.role})
  """.update

  def update(user: User, id: Long): Update0 = sql"""
    UPDATE USERS
    SET FIRST_NAME = ${user.firstName}, LAST_NAME = ${user.lastName},
        EMAIL = ${user.email}, HASH = ${user.hash}, PHONE = ${user.phone}, ROLE = ${user.role}
    WHERE ID = $id
  """.update

  def select(userId: Long): Query0[User] = sql"""
    SELECT FIRST_NAME, LAST_NAME, EMAIL, HASH, PHONE, ID, ROLE
    FROM USERS
    WHERE ID = $userId
  """.query

  def byUserEmail(email: String): Query0[User] = sql"""
    SELECT FIRST_NAME, LAST_NAME, EMAIL, HASH, PHONE, ID, ROLE
    FROM USERS
    WHERE EMAIL = $email
  """.query

  def delete(userId: Long): Update0 = sql"""
    DELETE FROM USERS WHERE ID = $userId
  """.update

  val selectAll: Query0[User] = sql"""
    SELECT FIRST_NAME, LAST_NAME, EMAIL, HASH, PHONE, ID, ROLE
    FROM USERS
  """.query
}
