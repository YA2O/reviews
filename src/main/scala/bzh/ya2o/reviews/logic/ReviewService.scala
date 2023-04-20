package bzh.ya2o.reviews.logic

import bzh.ya2o.reviews.integration.db.ReviewRepository
import bzh.ya2o.reviews.logic.ReviewService.ProductWithAverageRating
import bzh.ya2o.reviews.model.Review.ProductId
import cats.implicits._
import cats.Monad

import java.time.Instant

trait ReviewService[F[_]] {
  def findProductsWithBestReviews(
    start: Instant,
    end: Instant,
    nrReviewsMin: Int,
    nrResultsMax: Int
  ): F[Seq[ProductWithAverageRating]]
}
object ReviewService {
  case class ProductWithAverageRating(
    productId: ProductId,
    averageRating: BigDecimal
  )
}

class ReviewServiceImpl[F[_]](
  repo: ReviewRepository[F]
)(
  implicit F: Monad[F]
) extends ReviewService[F] {

  override def findProductsWithBestReviews(
    start: Instant,
    end: Instant,
    nrReviewsMin: Int,
    nrResultsMax: Int
  ): F[Seq[ProductWithAverageRating]] = {
    repo
      .reviewsInTimeRange(start, end)
      .map {
        _.foldLeft(Map.empty[ProductId, (Int, Int)]) { (map, review) =>
          map.updatedWith(key = review.productId) { maybeNrReviewsAndRatingSum: Option[(Int, Int)] =>
            (maybeNrReviewsAndRatingSum.getOrElse((0 -> 0)) |+| (1 -> review.rating.value)).some
          }
        }
          .filter { case (_, (nrReviews, _)) =>
            nrReviews >= nrReviewsMin
          }
          .toSeq
          .map { case (productId, (nrReviews, ratingSum)) =>
            ProductWithAverageRating(productId, averageRating = BigDecimal(ratingSum) / nrReviews)
          }
          .sortBy(-_.averageRating)
          .take(nrResultsMax)
      }
  }

}
