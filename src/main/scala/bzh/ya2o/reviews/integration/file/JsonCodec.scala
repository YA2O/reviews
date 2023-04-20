package bzh.ya2o.reviews.integration
package file

import bzh.ya2o.reviews.model.Review
import bzh.ya2o.reviews.model.Review.ProductId
import bzh.ya2o.reviews.model.Review.Rating
import cats.implicits._
import io.circe.Decoder

import java.time.Instant

object JsonCodec {

  implicit val reviewDecoder: Decoder[Review] = {
    implicit val productIdDecoder: Decoder[ProductId] = Decoder[String].emap(ProductId.apply)
    implicit val ratingDecoder: Decoder[Rating] = Decoder[Float].emap(f => Rating(f.toInt))

    implicit val instantDecoder: Decoder[Instant] = Decoder[Long].emap { s =>
      Either.catchNonFatal(Instant.ofEpochSecond(s)).leftMap { err =>
        s"Invalid unix time: [$s]; cause: [${err.getMessage}]"
      }
    }

    Decoder.forProduct3("asin", "overall", "unixReviewTime")(Review.apply)
  }

}
