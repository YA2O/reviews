package bzh.ya2o.reviews.application
package config

import cats.implicits._
import ciris._
import ciris.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString

final case class FileConfig(
  path: NonEmptyString
)

object FileConfig {
  def config[F[_]]: ConfigValue[F, FileConfig] =
    prop("file.path")
      .as[NonEmptyString]
      .default("./src/main/resources/amazon-reviews.json")
      .map(FileConfig.apply)
}
