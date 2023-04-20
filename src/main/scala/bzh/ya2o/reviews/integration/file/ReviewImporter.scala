package bzh.ya2o.reviews.integration
package file

import bzh.ya2o.reviews.application.error.InternalAppError
import bzh.ya2o.reviews.model.Review
import cats.effect.Resource
import cats.effect.Sync
import cats.implicits._

import scala.io.Source

trait ReviewImporter[F[_]] {
  def allReviews(): F[Iterator[Review]]
}

class ReviewImporterFromFile[F[_]](
  src: Source
)(
  implicit F: Sync[F]
) extends ReviewImporter[F] {

  override def allReviews(): F[Iterator[Review]] = F.delay {
    import JsonCodec._
    src
      .reset() // An iterator can only be consumed once; we need this call to `reset` to get a new fresh iterator each time.
      .getLines()
      .map { line =>
        io.circe.parser
          .decode[Review](line)
          .leftMap { err =>
            throw InternalAppError(s"Format error in file: [${err.toString}]") //fail fast!
          }
          .merge
      }
  }
}

object ReviewImporterFromFile {
  def resource[F[_]](filePath: String)(implicit F: Sync[F]): Resource[F, ReviewImporterFromFile[F]] = {
    Resource
      .make {
        F.delay {
          Source.fromFile(filePath)
        }
      } { reader =>
        F.delay(reader.close())
      }
      .map { src =>
        new ReviewImporterFromFile[F](src)
      }
  }
}
