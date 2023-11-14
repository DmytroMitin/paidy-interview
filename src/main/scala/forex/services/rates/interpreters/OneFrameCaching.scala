package forex.services.rates.interpreters

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._
import forex.domain.Rate
import forex.services.rates.{CachingAlgebra, NoCachingAlgebra}
import forex.services.rates.errors._
import scala.collection.mutable

class OneFrameCaching[F[_]: Sync](noCaching: NoCachingAlgebra[F]) extends CachingAlgebra[F] {

  private val cache = mutable.Map[Rate.Pair, Rate]()

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    cache.get(pair) match {
      case Some(rate) if rate.timestamp.isNotOlderThan5Minutes =>
        rate.asRight[Error].pure[F]
      case _ =>
        for {
          responses <- noCaching.get(pair)
        } yield {
          responses.headOption match {
            case Some(head) =>
              val newRate = Rate(pair, head.price, head.time_stamp)
              cache += (pair -> newRate)
              newRate.asRight[Error]
            case None =>
              Error.OneFrameLookupFailed("One-Frame service returned empty result").asLeft[Rate]
          }
        }
    }
}
