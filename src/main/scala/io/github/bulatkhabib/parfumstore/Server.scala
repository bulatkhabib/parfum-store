package io.github.bulatkhabib.parfumstore

import cats.effect._
import doobie.util.ExecutionContexts
import io.circe.config.parser
import org.http4s.server.{Router, Server => H4Server}
import org.http4s.implicits._
import io.github.bulatkhabib.parfumstore.config.{DatabaseConfig, ParfumStoreConfig}
import io.github.bulatkhabib.parfumstore.domain.authentication.Auth
import io.github.bulatkhabib.parfumstore.domain.carts.service.CartService
import io.github.bulatkhabib.parfumstore.domain.parfums.service.ParfumService
import io.github.bulatkhabib.parfumstore.domain.parfums.validation
import io.github.bulatkhabib.parfumstore.domain.users.service.UserService
import io.github.bulatkhabib.parfumstore.domain.users.validation.UserValidationInterpreter
import io.github.bulatkhabib.parfumstore.infrastructure.endpoint.cart.CartEndpoints
import io.github.bulatkhabib.parfumstore.infrastructure.endpoint.parfum.ParfumEndpoints
import io.github.bulatkhabib.parfumstore.infrastructure.endpoint.user.UserEndpoints
import io.github.bulatkhabib.parfumstore.infrastructure.repository.doobie.auth.DoobieAuthRepositoryInterpreter
import io.github.bulatkhabib.parfumstore.infrastructure.repository.doobie.cart.DoobieCartRepositoryInterpreter
import io.github.bulatkhabib.parfumstore.infrastructure.repository.doobie.parfum.DoobieParfumRepositoryInterpreter
import io.github.bulatkhabib.parfumstore.infrastructure.repository.doobie.user.DoobieUserRepositoryInterpreter
import org.http4s.server.blaze.BlazeServerBuilder
import tsec.authentication.SecuredRequestHandler
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt


object Server extends IOApp {
  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer : Sync]: Resource[F, H4Server[F]] =
   for {
     conf <- Resource.eval(parser.decodePathF[F, ParfumStoreConfig]("parfumstore"))
     serverEc <- ExecutionContexts.cachedThreadPool[F]
     connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.connections.poolSize)
     txnEc <- ExecutionContexts.cachedThreadPool[F]
     xa <- DatabaseConfig.dbTransactor(conf.db, connEc, Blocker.liftExecutionContext(txnEc))
     key <- Resource.eval(HMACSHA256.generateKey[F])
     authRepo = DoobieAuthRepositoryInterpreter[F, HMACSHA256](key, xa)
     parfumRepo = DoobieParfumRepositoryInterpreter[F](xa)
     cartRepo = DoobieCartRepositoryInterpreter[F](xa)
     userRepo = DoobieUserRepositoryInterpreter[F](xa)
     parfumValidation = validation.ParfumValidationInterpreter[F](parfumRepo)
     parfumService = ParfumService[F](parfumRepo, parfumValidation)
     userValidation = UserValidationInterpreter[F](userRepo)
     cartService = CartService[F](cartRepo,parfumService, parfumValidation)
     userService = UserService[F](userRepo, userValidation)
     authenticator = Auth.jwtAuthenticator[F, HMACSHA256](key, authRepo, userRepo)
     routeAuth = SecuredRequestHandler(authenticator)
     httpApp = Router(
       "/users" -> UserEndpoints
         .endpoints[F, BCrypt, HMACSHA256](userService, BCrypt.syncPasswordHasher[F], routeAuth),
       "/parfums" -> ParfumEndpoints.endpoints[F, HMACSHA256](parfumService, routeAuth),
       "/carts" -> CartEndpoints.endpoints[F, HMACSHA256](cartService, routeAuth)
     ).orNotFound
     _ <- Resource.eval(DatabaseConfig.initializeDb(conf.db))
     server <- BlazeServerBuilder[F](serverEc)
       .bindHttp(conf.server.port, conf.server.host)
       .withHttpApp(httpApp)
       .resource
   } yield server

  override def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)
}
