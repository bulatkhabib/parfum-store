package io.github.bulatkhabib.parfumstore

import cats.effect.IO
import io.github.bulatkhabib.parfumstore.domain.authentication.SignupRequest
import io.github.bulatkhabib.parfumstore.domain.parfums.model.{Parfum, ParfumStatus}
import io.github.bulatkhabib.parfumstore.domain.parfums.model.ParfumStatus.{Available, Unavailable}
import io.github.bulatkhabib.parfumstore.domain.users.model.{Role, User}
import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary
import tsec.authentication.AugmentedJWT
import tsec.common.SecureRandomId
import tsec.jws.mac.{JWTMac, JWTMacImpure}
import tsec.jwt.JWTClaims
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.{PasswordHash, PasswordHasher}

import java.time.Instant

trait Arbitraries {

  implicit val role = Arbitrary[Role](Gen.oneOf(Role.values.toIndexedSeq))

  implicit val user = Arbitrary[User] {
    for {
      firstName <- arbitrary[String]
      lastName <- arbitrary[String]
      email <- arbitrary[String]
      password <- arbitrary[String]
      phone <- arbitrary[String]
      id <- Gen.option(Gen.posNum[Long])
      role <- arbitrary[Role]
    } yield User(firstName, lastName, email, password, phone, id, role)
  }

  case class AdminUser(value: User)
  case class CustomerUser(value: User)
  case class PassHash(hasher: PasswordHash[String])

//  val crypto: PasswordHasher[IO, String]

//  implicit val crypt: Arbitrary[PassHash] = Arbitrary {
//    for {
//      hash <- crypto.hashpw(user.arbitrary.map(u => u.hash))
//    } yield hash
//  }

  implicit val adminUser: Arbitrary[AdminUser] = Arbitrary {
    user.arbitrary.map(user => AdminUser(user.copy(role = Role.Admin)))
  }

  implicit val customerUser: Arbitrary[CustomerUser] = Arbitrary {
    user.arbitrary.map(user => CustomerUser(user.copy(role = Role.Customer)))
  }

  implicit val userSignup = Arbitrary[SignupRequest] {
    for {
      firstName <- arbitrary[String]
      lastName <- arbitrary[String]
      email <- arbitrary[String]
      password <- arbitrary[String]
      phone <- arbitrary[String]
    } yield SignupRequest(firstName, lastName, email, password, phone)
  }

  implicit val parfumStatus = Arbitrary[ParfumStatus] {
    Gen.oneOf(Available, Unavailable)
  }

  implicit val parfum = Arbitrary[Parfum] {
    for {
      name <- Gen.nonEmptyListOf(Gen.asciiPrintableChar).map(_.mkString)
      category <- arbitrary[String]
      description <- arbitrary[String]
      price <- Gen.choose(1000, 10000)
      status <- arbitrary[ParfumStatus]
      id <- Gen.option(Gen.posNum[Long])
    } yield Parfum(name, category, description, price, status, id)
  }

  implicit val secureRandomId = Arbitrary[SecureRandomId] {
    arbitrary[String].map(SecureRandomId.apply)
  }

  implicit val jwtMac: Arbitrary[JWTMac[HMACSHA256]] = Arbitrary {
    for {
      key <- Gen.const(HMACSHA256.unsafeGenerateKey)
      claims <- Gen.finiteDuration.map(exp =>
        JWTClaims.withDuration[IO](expiration = Some(exp)).unsafeRunSync(),
      )
    } yield JWTMacImpure
      .build[HMACSHA256](claims, key)
      .getOrElse(throw new Exception("Inconceivable"))
  }

  implicit def augmentedJWT[A, I](implicit
                                  arb1: Arbitrary[JWTMac[A]],
                                  arb2: Arbitrary[I],
                                 ): Arbitrary[AugmentedJWT[A, I]] =
    Arbitrary {
      for {
        id <- arbitrary[SecureRandomId]
        jwt <- arb1.arbitrary
        identity <- arb2.arbitrary
        expiry <- arbitrary[Instant]
        lastTouched <- Gen.option(arbitrary[Instant])
      } yield AugmentedJWT(id, jwt, identity, expiry, lastTouched)
    }
}

object Arbitraries extends Arbitraries