package forex.services.rates.interpreters

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.show._
import forex.domain.Rate
import forex.http.rates.Protocol.GetOneFrameApiResponse
import forex.services.rates.NoCachingAlgebra
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.{Header, Request}
import org.log4s.getLogger

class OneFrameNoCaching[F[_]: Sync](client: Client[F]) extends NoCachingAlgebra[F] {

  private val logger = getLogger

  // TODO make domain, port, token configurable
  override def get(pair: Rate.Pair): F[List[GetOneFrameApiResponse]] = {
    val request = Request[F](
      uri = uri"http://localhost:8080/rates" +? ("pair", pair.from.show + pair.to.show)
    ).putHeaders(Header("token", "10dc303535874aeccc86a8251e6992f5"))
    logger.info(s"One-frame request: $request")

    for {
      response <- client.expect[List[GetOneFrameApiResponse]](request)
    } yield {
      logger.info(s"One-frame response: $response")
      response
    }
  }
}
