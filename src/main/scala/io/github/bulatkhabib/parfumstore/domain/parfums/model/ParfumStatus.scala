package io.github.bulatkhabib.parfumstore.domain.parfums.model

import enumeratum._

sealed trait ParfumStatus extends EnumEntry

case object ParfumStatus extends Enum[ParfumStatus] with CirceEnum[ParfumStatus] {
  case object Available extends ParfumStatus
  case object Unavailable extends ParfumStatus

  val values = findValues
}
