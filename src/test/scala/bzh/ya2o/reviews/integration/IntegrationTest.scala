package bzh.ya2o.reviews.integration

import bzh.ya2o.reviews.application.logging.Logger
import bzh.ya2o.reviews.Main
import bzh.ya2o.reviews.application.config.FileConfig
import cats.effect.IO
import cats.effect.Resource
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.literal._
import io.circe.Json
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._

class IntegrationTest extends CatsEffectSuite {
  private[this] val dsl = new Http4sDsl[IO] {}
  import dsl._
  implicit val entityEncoder: EntityEncoder[IO, Json] =
    CirceEntityEncoder.circeEntityEncoder

  val webApiResource: Resource[IO, Request[IO] => IO[Response[IO]]] = {
    val config = FileConfig(NonEmptyString.unsafeFrom("./src/test/resources/test-data.json"))
    val logger = new Logger[IO] {
      override def error(throwable: Throwable): IO[Unit] = ???
      override def warn(msg: String): IO[Unit] = ???
      override def info(msg: String): IO[Unit] = ???
      override def debug(msg: String): IO[Unit] = ???
    }
    Main.mkWebApi[IO](logger, config).map(_.routes().orNotFound.run)
  }

  test("produce same response as in specification") {
    webApiResource.use { webApi =>
      // Arrange
      val json = json"""
      {
        "start": "01.01.2010",
        "end": "31.12.2020",
        "limit": 2,
        "min_number_reviews": 2
      }"""

      // Act
      val respIO: IO[Response[IO]] = webApi(
        Request[IO](
          method = POST,
          uri = uri"/amazon/best-rated",
          body = entityEncoder.toEntity(json).body
        )
      )

      // Assert
      val expectedJson = json"""
      [
        {
            "asin": "B000JQ0JNS",
            "average_rating": 4.5
        },
        {
            "asin": "B000NI7RW8",
            "average_rating": 3.666666666666666666666666666666667
        }
      ]"""
      respIO.flatMap(_.asJson).assertEquals(expectedJson) *>
        respIO.map(_.status).assertEquals(Status.Ok)
    }
  }

}
