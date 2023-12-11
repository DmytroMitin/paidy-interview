package forex.services.rates

import cats.Applicative
import cats.effect.{Concurrent, Deferred, Ref}
import forex.domain.Rate
import interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: CachingAlgebra[F] = new OneFrameDummy[F]()

  def caching[F[_]: Concurrent](
      noCaching: NoCachingAlgebra[F],
      cache: Ref[F, Deferred[F, Map[Rate.Pair, Rate]]]
  ): CachingAlgebra[F] =
    new OneFrameCaching[F](noCaching, cache)

}
