package bzh.ya2o.reviews

import bzh.ya2o.reviews.application.config._
import bzh.ya2o.reviews.application.logging._
import bzh.ya2o.reviews.integration.db.ReviewRepositoryImpl
import bzh.ya2o.reviews.integration.file.ReviewImporterFromFile
import bzh.ya2o.reviews.integration.webapi._
import bzh.ya2o.reviews.logic._
import cats.effect.Async
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import org.http4s.server.Server
import org.log4s.getLogger

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val logger: Logger[IO] = new Logger4j[IO](getLogger)
    mkServer[IO](logger).useForever
      .onError { throwable =>
        logger.error(throwable)
      }
      .as(ExitCode.Success)
  }

  def mkServer[F[_]: Async](logger: Logger[F]): Resource[F, Server] = {
    for {
      config <- mkConfig[F](logger)
      webApi <- mkWebApi[F](logger, config.file)
      server <- Server.resource[F](webApi, config.server)
    } yield server
  }

  def mkConfig[F[_]: Async](logger: Logger[F]): Resource[F, Config] = {
    for {
      config <- Resource.eval(Config.config[F].load)
      _ <- Resource.eval(logger.info(s"Config: [$config]"))
    } yield config
  }

  def mkWebApi[F[_]: Async](logger: Logger[F], config: FileConfig): Resource[F, WebApi[F]] = {
    for {
      service <- mkService[F](config)
      webApi = new WebApi[F](service, logger)
    } yield webApi
  }

  def mkService[F[_]: Async](config: FileConfig): Resource[F, ReviewService[F]] = {
    for {
      importer <- ReviewImporterFromFile.resource[F](config.path.value)
      repo = new ReviewRepositoryImpl[F](importer)
    } yield new ReviewServiceImpl[F](repo)
  }

}
