package bzh.ya2o.reviews.logic

//import bzh.ya2o.reviews.application.error.InternalAppError
import bzh.ya2o.reviews.application.error.InternalAppError
import bzh.ya2o.reviews.integration.db.ReviewRepositoryImpl
import bzh.ya2o.reviews.integration.file.ReviewImporterFromFile
import bzh.ya2o.reviews.logic.ReviewService.ProductWithAverageRating
import bzh.ya2o.reviews.model.Review.ProductId
import cats.effect.IO
import munit.CatsEffectSuite

import java.time.Instant
import scala.io.Source

class ReviewServiceImplTest extends CatsEffectSuite {

  test("find the product with best average rating") {
    // Arrange
    val fx = new TestFixture {}
    // Act
    val result: IO[Seq[ReviewService.ProductWithAverageRating]] =
      fx.service.findProductsWithBestReviews(
        Instant.parse("2000-01-01T00:00:00.000Z"),
        Instant.parse("2020-12-31T23:59:59.999Z"),
        nrReviewsMin = 2,
        nrResultsMax = 1
      )
    // Assert
    result.assertEquals(
      List(
        ProductWithAverageRating(
          ProductId("A").toOption.get,
          BigDecimal(4.5)
        )
      )
    )
  }

  test("raise an InternalAppError when the review data in invalid") {
    // Arrange
    val fx = new TestFixture {
      override lazy val reviewData = """{"asin":"---"}"""
    }
    // Act
    val result: IO[Seq[ReviewService.ProductWithAverageRating]] =
      fx.service.findProductsWithBestReviews(
        Instant.parse("2000-01-01T00:00:00.000Z"),
        Instant.parse("2020-12-31T23:59:59.999Z"),
        nrReviewsMin = 2,
        nrResultsMax = 1
      )
    // Assert
    result.intercept[InternalAppError]
  }

  trait TestFixture {
    lazy val reviewData =
      """{"asin":"A","overall":4.0,"unixReviewTime":1475261866}
        |{"asin":"B","overall":3.0,"unixReviewTime":1455120950}
        |{"asin":"C","overall":5.0,"unixReviewTime":1571581258}
        |{"asin":"A","overall":5.0,"unixReviewTime":1466668179}
        |{"asin":"B","overall":4.0,"unixReviewTime":1466668179}
        |""".stripMargin
    lazy val src = Source.fromString(reviewData)
    lazy val importer = new ReviewImporterFromFile[IO](src)
    lazy val repo = new ReviewRepositoryImpl[IO](importer)
    lazy val service = new ReviewServiceImpl[IO](repo)
  }
}
