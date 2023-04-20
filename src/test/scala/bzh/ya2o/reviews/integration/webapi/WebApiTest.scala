package bzh.ya2o.reviews
package integration
package webapi

import bzh.ya2o.reviews.application.error.InternalAppError
import bzh.ya2o.reviews.application.logging.Logger
import bzh.ya2o.reviews.logic.ReviewService
import bzh.ya2o.reviews.logic.ReviewService.ProductWithAverageRating
import bzh.ya2o.reviews.model.Review
import cats.effect.IO
import cats.implicits._
import io.circe.literal._
import io.circe.Json
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._

import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class WebApiTest extends CatsEffectSuite {
  private[this] val dsl = new Http4sDsl[IO] {}
  import dsl._
  implicit val entityEncoder: EntityEncoder[IO, Json] =
    CirceEntityEncoder.circeEntityEncoder

  test("respond with 200 and result from review service") {
    // Arrange
    val fx = new TestFixture {}
    val requestJson = json"""
      {
        "start": "01.01.2010",
        "end": "31.12.2020",
        "limit": 2,
        "min_number_reviews": 2
      }"""

    // Act
    val respIO: IO[Response[IO]] = fx.webApi(
      Request[IO](
        method = POST,
        uri = uri"/amazon/best-rated",
        body = entityEncoder.toEntity(requestJson).body
      )
    )

    // Assert
    respIO.map(_.status).assertEquals(Status.Ok) *> {
      val expectedResponseJson = json"""[{"asin": ${fx.productId_}, "average_rating": ${fx.avgRating_}}]"""
      respIO.flatMap(_.asJson).assertEquals(expectedResponseJson)
    }
  }

  test("respond with 400 for invalid request body") {
    // Arrange
    val fx = new TestFixture {}
    val requestJson = json"""{}"""

    // Act
    val respIO: IO[Response[IO]] = fx.webApi(
      Request[IO](
        method = POST,
        uri = uri"/amazon/best-rated",
        body = entityEncoder.toEntity(requestJson).body
      )
    )

    // Assert
    respIO.map(_.status).assertEquals(Status.BadRequest)
  }

  test("respond with 500 if an InternalAppError is raised") {
    // Arrange
    val fx = new TestFixture {
      override lazy val service: ReviewService[IO] = new ReviewService[IO] {
        override def findProductsWithBestReviews(
          start: Instant,
          end: Instant,
          nrReviewsMin: Int,
          nrResultsMax: Int
        ): IO[Seq[ProductWithAverageRating]] = IO.raiseError(InternalAppError("internal error"))
      }
    }
    val requestJson = json"""
      {
        "start": "01.01.2010",
        "end": "31.12.2020",
        "limit": 2,
        "min_number_reviews": 2
      }"""

    // Act
    val respIO: IO[Response[IO]] = fx.webApi(
      Request[IO](
        method = POST,
        uri = uri"/amazon/best-rated",
        body = entityEncoder.toEntity(requestJson).body
      )
    )

    // Assert
    respIO.map(_.status).assertEquals(Status.InternalServerError)
  }

  trait TestFixture {
    private val isoLocalDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    lazy val start_ = LocalDateTime
      .of(LocalDate.from(isoLocalDateFormatter.parse("2010-01-01")), LocalTime.MIN)
      .toInstant(ZoneOffset.UTC)
    lazy val end_ = LocalDateTime
      .of(LocalDate.from(isoLocalDateFormatter.parse("2020-12-31")), LocalTime.MAX)
      .toInstant(ZoneOffset.UTC)
    lazy val nrReviewsMin_ = 2
    lazy val nrResultsMax_ = 2

    lazy val productId_ = "ABC"
    lazy val avgRating_ = 1.23

    lazy val logger = new Logger[IO] {
      override def error(throwable: Throwable): IO[Unit] = IO.unit
      override def warn(msg: String): IO[Unit] = IO.unit
      override def info(msg: String): IO[Unit] = IO.unit
      override def debug(msg: String): IO[Unit] = IO.unit
    }

    lazy val service = new ReviewService[IO]() {
      override def findProductsWithBestReviews(
        start: Instant,
        end: Instant,
        nrReviewsMin: Int,
        nrResultsMax: Int
      ): IO[Seq[ReviewService.ProductWithAverageRating]] = {
        if (
          start === start_ &&
          end === end_ &&
          nrReviewsMin === nrReviewsMin_ &&
          nrResultsMax === nrResultsMax_
        )
          IO.pure(
            List(
              ProductWithAverageRating(
                Review.ProductId(productId_).toOption.get,
                BigDecimal(avgRating_)
              )
            )
          )
        else ???
      }
    }
    lazy val webApi = new WebApi[IO](service, logger).routes().orNotFound.run
  }

}
