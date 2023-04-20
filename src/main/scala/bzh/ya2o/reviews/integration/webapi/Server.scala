package bzh.ya2o.reviews.integration
package webapi

import bzh.ya2o.reviews.application.config._
import cats.effect.Async
import cats.effect.Resource
import com.comcast.ip4s.Port
import org.http4s.server.middleware.Logger
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.Server

object Server {

  def resource[F[_]](
    webApi: WebApi[F],
    config: ServerConfig
  )(
    implicit F: Async[F]
  ): Resource[F, Server] = {

    val httpApp: HttpApp[F] = Logger.httpApp(
      logHeaders = true,
      logBody = true
    )(
      webApi.routes().orNotFound
    )

    for {
      port <- Resource.pure(
        Port
          .fromInt(config.port.value)
          .getOrElse(throw new IllegalStateException(s"Invalid port: [${config.port}]."))
      )
      server <- EmberServerBuilder
        .default[F]
        .withPort(port)
        .withHttpApp(httpApp)
        .build
    } yield server
  }

}
