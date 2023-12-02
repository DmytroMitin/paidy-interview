package forex.services.rates

import errors._
import forex.domain.Rate
import forex.http.rates.Protocol.GetOneFrameApiResponse

trait CachingAlgebra[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Rate]
}

trait NoCachingAlgebra[F[_]] {
  def get(pairs: List[Rate.Pair]): F[List[GetOneFrameApiResponse]]
}
