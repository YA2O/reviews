package bzh.ya2o

import cats.Eq

import java.time.Instant

package object reviews {
  implicit val eqInstant: Eq[Instant] = Eq.fromUniversalEquals
}
