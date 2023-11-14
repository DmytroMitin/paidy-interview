package forex.services.rates.interpreters

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.show._
import forex.domain.Rate
import forex.http.rates.Protocol.GetOneFrameApiResponse
import forex.services.rates.NoCachingAlgebra
import org.http4s.Uri.{Authority, RegName}
import org.http4s.client.Client
import org.http4s.{Header, Request, Uri}
import org.log4s.getLogger

class OneFrameNoCaching[F[_]: Sync](
                                     client: Client[F],
                                     oneFrameHost: String,
                                     oneFramePort: Int,
                                     oneFrameToken: String
                                   ) extends NoCachingAlgebra[F] {

  private val logger = getLogger

  override def get(pair: Rate.Pair): F[List[GetOneFrameApiResponse]] = {
    val oneFrameAuthority = Some(Authority(host = RegName(oneFrameHost), port = Some(oneFramePort)))
    val request = Request[F](
      uri = Uri(authority = oneFrameAuthority) / "rates" +? ("pair", pair.from.show + pair.to.show)
    ).putHeaders(Header("token", oneFrameToken))
    logger.info(s"One-frame request: $request")

    for {
      response <- client.expect[List[GetOneFrameApiResponse]](request)
    } yield {
      logger.info(s"One-frame response: $response")
      response
    }
  }
}
