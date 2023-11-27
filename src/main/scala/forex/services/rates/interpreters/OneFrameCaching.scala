package forex.services.rates.interpreters

import cats.effect.Concurrent
import cats.effect.concurrent.MVar2
import cats.syntax.flatMap._
import cats.syntax.either._
import cats.syntax.functor._
import forex.domain.Rate
import forex.http.rates.Protocol.GetOneFrameApiResponse
import forex.services.rates.{CachingAlgebra, NoCachingAlgebra}
import forex.services.rates.errors._

class OneFrameCaching[F[_]: Concurrent](
                                         noCaching: NoCachingAlgebra[F],
                                         cache: MVar2[F, Map[Rate.Pair, Rate]]
                                       ) extends CachingAlgebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    def getHlp(map: Map[Rate.Pair, Rate]): F[Error Either Rate] = {
      def getHlp1(responses: List[GetOneFrameApiResponse]): F[Error Either Rate] = responses.headOption match {
        case Some(head) =>
          val newRate = Rate(pair, head.price, head.time_stamp)
          for {
            _ <- cache.put(map + (pair -> newRate))
          } yield newRate.asRight[Error]
        case None =>
          for {
            _ <- cache.put(map)
          } yield Error.OneFrameLookupFailed("One-Frame service returned empty result").asLeft[Rate]
      }

      map.get(pair) match {
        case Some(rate) if rate.timestamp.isNotOlderThan5Minutes =>
          for {
            _ <- cache.put(map)
          } yield rate.asRight[Error]
        case _ =>
          for {
            responses <- noCaching.get(pair)
            res <- getHlp1(responses)
          } yield res
      }
    }

    for {
      map <- cache.take
      res <- getHlp(map)
    } yield res
  }
}
