package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: CachingAlgebra[F] = new OneFrameDummy[F]()

  def caching[F[_]: Sync](noCaching: NoCachingAlgebra[F]): CachingAlgebra[F] = new OneFrameCaching[F](noCaching)
}
