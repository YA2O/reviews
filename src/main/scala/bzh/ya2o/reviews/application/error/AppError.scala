package bzh.ya2o.reviews.application
package error

import scala.util.control.NoStackTrace

sealed trait AppError extends Throwable with NoStackTrace {
  def message: String
  override def getMessage: String = message
}

final case class InternalAppError(message: String) extends AppError
