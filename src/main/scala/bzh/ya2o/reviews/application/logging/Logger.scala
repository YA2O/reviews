package bzh.ya2o.reviews.application
package logging

import cats.effect.Sync

trait Logger[F[_]] {
  def error(throwable: Throwable): F[Unit]
  def warn(msg: String): F[Unit]
  def info(msg: String): F[Unit]
  def debug(msg: String): F[Unit]
}

class Logger4j[F[_]](logger: org.log4s.Logger)(implicit F: Sync[F]) extends Logger[F] {

  override def error(throwable: Throwable): F[Unit] = F.delay(logger.error(throwable)(throwable.getMessage))

  override def warn(msg: String): F[Unit] = F.delay(logger.warn(msg))

  override def info(msg: String): F[Unit] = F.delay(logger.info(msg))

  override def debug(msg: String): F[Unit] = F.delay(logger.debug(msg))
}
