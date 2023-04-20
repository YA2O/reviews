package bzh.ya2o.reviews
package integration
package db

import bzh.ya2o.reviews.application.error.InternalAppError
import bzh.ya2o.reviews.integration.file.ReviewImporter
import bzh.ya2o.reviews.model.Review
import cats.effect._
import cats.implicits._

import java.time.Instant

trait ReviewRepository[F[_]] {
  def reviewsInTimeRange(start: Instant, end: Instant): F[Iterator[Review]]
}

class ReviewRepositoryImpl[F[_]](
  importer: ReviewImporter[F]
)(
  implicit F: Sync[F]
) extends ReviewRepository[F] {

  // Note: returns the reviews with a timestamp such that 'start ≤ timestamp < end'.
  override def reviewsInTimeRange(start: Instant, end: Instant): F[Iterator[Review]] = {
    if (start.isAfter(end))
      F.raiseError(
        InternalAppError(
          s"Illegal arguments to reviewRepository#reviewsInTimeRange: start [$start] is after end [$end]!"
        )
      )
    else
      importer.allReviews().map {
        _.filter { review =>
          val timestamp = review.timestamp
          (start.isBefore(timestamp) || start === timestamp) && timestamp.isBefore(end)
        }
      }
  }

}
