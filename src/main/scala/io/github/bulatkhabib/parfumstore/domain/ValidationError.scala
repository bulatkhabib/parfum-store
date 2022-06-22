package io.github.bulatkhabib.parfumstore.domain

import io.github.bulatkhabib.parfumstore.domain.carts.model.Cart
import io.github.bulatkhabib.parfumstore.domain.parfums.model.Parfum
import io.github.bulatkhabib.parfumstore.domain.users.model.User

sealed trait ValidationError extends Product with Serializable
case class ParfumAlreadyExistsError(parfum: Parfum) extends ValidationError
case object ParfumNotFoundError extends ValidationError
case object ParfumStatusIsUnavailable extends ValidationError
case object CartNotFoundError extends ValidationError
case object OrderNotFoundError extends ValidationError
case object UserNotFoundError extends ValidationError
case class UserAlreadyExistsError(user: User) extends ValidationError
case class UserAuthenticationFailedError(email: String) extends ValidationError
