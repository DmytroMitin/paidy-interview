package forex.services.rates.interpreters

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.show._
import forex.domain.Rate
import forex.http.rates.Protocol.GetOneFrameApiResponse
import forex.services.rates.Algebra
import forex.services.rates.errors._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.{Header, Request}
import org.log4s.getLogger
import scala.collection.mutable

class OneFrame[F[_]: Sync](client: Client[F]) extends Algebra[F] {

  private val logger = getLogger

  private val cache = mutable.Map[Rate.Pair, Rate]()

  // TODO make domain, port, token configurable
  override def get(pair: Rate.Pair): F[Error Either Rate] =
    cache.get(pair) match {
      case Some(rate) if rate.timestamp.isNotOlderThan5Minutes =>
        rate.asRight[Error].pure[F]
      case _ =>
        val request = Request[F](
          uri = uri"http://localhost:8080/rates" +? ("pair", pair.from.show + pair.to.show)
        ).putHeaders(Header("token", "10dc303535874aeccc86a8251e6992f5"))
        logger.info(s"One-frame request: $request")

        for {
          response <- client.expect[List[GetOneFrameApiResponse]](request)
        } yield {
          logger.info(s"One-frame response: $response")
          response.headOption match {
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
