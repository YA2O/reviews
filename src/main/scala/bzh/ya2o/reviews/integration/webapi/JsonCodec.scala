package bzh.ya2o.reviews.integration
package webapi

import bzh.ya2o.reviews.logic.ReviewService.ProductWithAverageRating
import bzh.ya2o.reviews.model.Review.ProductId
import cats.implicits._
import io.circe.Decoder
import io.circe.Encoder

import java.time.format.DateTimeFormatter
import java.time.LocalDate

object JsonCodec {

  object Input {

    case class BestRated(
      start: LocalDate,
      end: LocalDate,
      nrResultsMax: Int,
      nrReviewsMin: Int
    )

    object BestRated {
      def apply(
        start: LocalDate,
        end: LocalDate,
        nrResultsMax: Int,
        nrReviewsMin: Int
      ): Either[String, BestRated] = {
        def checkIsDateRangeValid(start: LocalDate, end: LocalDate): Either[String, (LocalDate, LocalDate)] =
          if (start.isAfter(end)) s"Invalid dates: start [$start] is after end [$end]!".asLeft
          else (start, end).asRight

        def checkIsPositive(n: Int): Either[String, Int] =
          if (n >= 0) n.asRight else s"Invalid number[$n]: not positive!".asLeft

        (checkIsDateRangeValid(start, end), checkIsPositive(nrResultsMax), checkIsPositive(nrReviewsMin))
          .parMapN { case ((s, e), a, b) =>
            new BestRated(s, e, a, b)
          }
      }

      implicit val bestRatedInputDecoder: Decoder[BestRated] = {
        implicit val localDateDecoder: Decoder[LocalDate] = Decoder[String].emap { s =>
          val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
          Either.catchNonFatal(LocalDate.from(formatter.parse(s))).leftMap { err =>
            s"Invalid date [$s]; cause: [${err.getMessage}]"
          }
        }
        Decoder
          .forProduct4("start", "end", "limit", "min_number_reviews") {
            (a: LocalDate, b: LocalDate, c: Int, d: Int) => (a, b, c, d)
          }
          .emap(Function.tupled(BestRated.apply _)(_))
      }
    }
  }

  object Output {
    implicit val productWithAverageRatingEncoder: Encoder[ProductWithAverageRating] = {
      implicit val productIdEncoder: Encoder[ProductId] = Encoder[String].contramap(_.value)
      Encoder.forProduct2("asin", "average_rating") { prod =>
        (prod.productId, prod.averageRating)
      }
    }
  }

}
