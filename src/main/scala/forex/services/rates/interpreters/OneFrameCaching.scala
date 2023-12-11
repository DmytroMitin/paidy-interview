package forex.services.rates.interpreters

import cats.effect.{Concurrent, Deferred, Ref}
import cats.syntax.flatMap._
import cats.syntax.either._
import cats.syntax.functor._
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.http.rates.Protocol.GetOneFrameApiResponse
import forex.services.rates.{CachingAlgebra, NoCachingAlgebra}
import forex.services.rates.errors._

class OneFrameCaching[F[_]: Concurrent](
                                         noCaching: NoCachingAlgebra[F],
                                         cache: Ref[F, Deferred[F, Map[Rate.Pair, Rate]]]
                                       ) extends CachingAlgebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    def getFromCacheOrResponse(map: Map[Rate.Pair, Rate]): F[Error Either Rate] = {
      def getFromResponse(responses: List[GetOneFrameApiResponse]): F[Error Either Rate] = {
        val newMap = responses.foldLeft(map)((currentMap, response) => {
          val currentPair = Pair(response.from, response.to)
          val newRate = Rate(currentPair, response.price, response.time_stamp)
          currentMap + (currentPair -> newRate)
        })

        for {
          deferred <- Deferred[F, Map[Rate.Pair, Rate]]
          _ <- deferred.complete(newMap)
          _ <- cache.set(deferred)
        } yield newMap.get(pair).toRight(
          Error.OneFrameLookupFailed(s"One-Frame service returned no result for pair $pair")
        )
      }

      map.get(pair) match {
        case Some(rate) if rate.timestamp.isNotOlderThan5Minutes =>
          for {
            deferred <- Deferred[F, Map[Rate.Pair, Rate]]
            _ <- deferred.complete(map)
            _ <- cache.set(deferred)
          } yield rate.asRight[Error]
        case _ =>
          for {
            responses <- noCaching.get(Pair.values)
            res <- getFromResponse(responses)
          } yield res
      }
    }

    for {
      d <- cache.get
      map <- d.get
      res <- getFromCacheOrResponse(map)
    } yield res
  }
}
