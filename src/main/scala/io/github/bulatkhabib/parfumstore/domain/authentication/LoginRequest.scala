package io.github.bulatkhabib.parfumstore.domain.authentication

import io.github.bulatkhabib.parfumstore.domain.users.model.{Role, User}
import tsec.passwordhashers.PasswordHash

final case class LoginRequest(
                               email: String,
                               password: String
                             )

final case class SignupRequest(
                                firstName: String,
                                lastName: String,
                                email: String,
                                password: String,
                                phone: String
                              ) {
  def asUser[A](hasherPassword: PasswordHash[A]): User = User(
    firstName,
    lastName,
    email,
    hasherPassword,
    phone,
    role = Role("Customer")
  )
}