package io.github.bulatkhabib.parfumstore.domain.users.model

import cats._
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

final case class Role(roleRepr: String)

object Role extends SimpleAuthEnum[Role, String] {

  val Admin: Role = Role("Admin")
  val Customer: Role = Role("Customer")

  override def getRepr(t: Role): String = t.roleRepr

  override protected val values: AuthGroup[Role] = AuthGroup(Admin, Customer)

  implicit val eqRole: Eq[Role] = Eq.fromUniversalEquals[Role]
}
