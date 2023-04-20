package bzh.ya2o.reviews.model

import bzh.ya2o.reviews.model.Review._

import java.time.Instant

final case class Review(
  productId: ProductId,
  rating: Rating,
  timestamp: Instant
)

object Review {
  sealed abstract case class ProductId(value: String)
  object ProductId {
    private val regex = """^[A-Z0-9]{1,64}$""".r
    def apply(value: String): Either[String, ProductId] = {
      if (isValid(value))
        Right(unsafeApply(value))
      else
        Left(s"Invalid productId [$value]!")
    }
    private def isValid(value: String): Boolean = regex.matches(value)
    private def unsafeApply(value: String): ProductId = new ProductId(value) {}
  }

  sealed abstract case class Rating(value: Int)
  object Rating {
    def apply(value: Int): Either[String, Rating] = {
      if (isValid(value)) Right(unsafeApply(value))
      else Left(s"Invalid rating: [$value]!")
    }
    private def isValid(value: Int): Boolean = value >= 0 && value <= 5
    private def unsafeApply(value: Int): Rating = new Rating(value) {}
  }

}
