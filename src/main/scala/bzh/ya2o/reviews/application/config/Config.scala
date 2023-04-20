package bzh.ya2o.reviews.application
package config

import cats.implicits._

import ciris.ConfigValue

final case class Config(
  file: FileConfig,
  server: ServerConfig
)

object Config {
  def config[F[_]]: ConfigValue[F, Config] = {
    (FileConfig.config, ServerConfig.config).parMapN(Config.apply)
  }
}
