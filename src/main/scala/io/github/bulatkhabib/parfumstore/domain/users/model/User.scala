package io.github.bulatkhabib.parfumstore.domain.users.model

import cats.Applicative
import tsec.authorization.AuthorizationInfo

case class User(
               firstName: String,
               lastName: String,
               email: String,
               hash: String,
               phone: String,
               id: Option[Long] = None,
               role: Role
               )
{
  def toUserWithoutHash: UserWithoutHash = {
    UserWithoutHash(firstName, lastName, email, phone, id, role)
  }
}

case class UserWithoutHash(
                          firstName: String,
                          lastName: String,
                          email: String,
                          phone: String,
                          id: Option[Long] = None,
                          role: Role
                          )

object User {
  implicit def authRole[F[_]](implicit F: Applicative[F]): AuthorizationInfo[F, Role, User] = (u: User) => F.pure(u.role)
}
