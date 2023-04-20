package bzh.ya2o.reviews.integration
package webapi

import bzh.ya2o.reviews.application.error.AppError
import bzh.ya2o.reviews.application.error.InternalAppError
import bzh.ya2o.reviews.application.logging._
import bzh.ya2o.reviews.logic.ReviewService
import bzh.ya2o.reviews.logic.ReviewService.ProductWithAverageRating
import cats.effect.Async
import cats.implicits._
import io.circe.Encoder
import org.http4s.dsl.Http4sDsl
import org.http4s.EntityDecoder
import org.http4s.HttpRoutes
import org.http4s.Response
import org.http4s.circe.CirceEntityEncoder
import org.http4s.EntityEncoder
import org.http4s.Request
import org.http4s.circe

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class WebApi[F[_]](
  service: ReviewService[F],
  logger: Logger[F]
)(
  implicit F: Async[F]
) {
  private[this] val dsl = new Http4sDsl[F] {}
  import dsl._
  import JsonCodec._
  import JsonCodec.Output._

  def routes(): HttpRoutes[F] = {
    HttpRoutes.of[F] { case req @ POST -> Root / "amazon" / "best-rated" =>
      withErrorHandling(findReviews(req))
    }
  }

  private[this] def withErrorHandling(responseF: F[Response[F]]): F[Response[F]] = {
    case class ErrorMessageOutput(msg: String, cause: String)
    object ErrorMessageOutput {
      implicit val encoder: Encoder[ErrorMessageOutput] =
        Encoder.forProduct2("message", "cause")(err => (err.msg, err.cause))
    }
    implicit val entityEncoder: EntityEncoder[F, ErrorMessageOutput] =
      CirceEntityEncoder.circeEntityEncoder

    responseF.handleErrorWith {
      case err: org.http4s.InvalidMessageBodyFailure =>
        BadRequest(ErrorMessageOutput(err.getMessage, err.getCause.getMessage))
      case err: AppError =>
        err match {
          case InternalAppError(msg) =>
            logger.error(err) >>
              InternalServerError(ErrorMessageOutput("Internal error!", msg))
        }
      case throwable: Throwable =>
        logger.error(throwable) >>
          InternalServerError(ErrorMessageOutput(s"Unhandled error!", throwable.toString))
    }
  }

  private[this] def findReviews(req: Request[F]): F[Response[F]] = {
    implicit val inputDecoder: EntityDecoder[F, Input.BestRated] =
      circe.accumulatingJsonOf[F, Input.BestRated]

    implicit val outputEncoder: EntityEncoder[F, Seq[ProductWithAverageRating]] =
      CirceEntityEncoder.circeEntityEncoder

    for {
      input <- req.as[Input.BestRated]
      result <- service
        .findProductsWithBestReviews(
          start = LocalDateTime.of(input.start, LocalTime.MIN).toInstant(ZoneOffset.UTC),
          end = LocalDateTime.of(input.end, LocalTime.MAX).toInstant(ZoneOffset.UTC),
          nrReviewsMin = input.nrReviewsMin,
          nrResultsMax = input.nrResultsMax
        )
      resp <- Ok(result)
    } yield resp
  }

}
