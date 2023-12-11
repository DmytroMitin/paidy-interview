package forex.services.rates

import cats.Applicative
import cats.effect.std.Queue
import cats.effect.Concurrent
import forex.domain.Rate
import interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: CachingAlgebra[F] = new OneFrameDummy[F]()

  def caching[F[_]: Concurrent](
      noCaching: NoCachingAlgebra[F],
      cache: Queue[F, Map[Rate.Pair, Rate]]
  ): CachingAlgebra[F] =
    new OneFrameCaching[F](noCaching, cache)

}
