package forex.services.rates.interpreters

import cats.effect.kernel.Concurrent
import cats.syntax.functor._
import cats.syntax.show._
import forex.domain.Rate
import forex.http._
import forex.http.rates.Protocol.GetOneFrameApiResponse
import forex.services.rates.NoCachingAlgebra
import org.http4s.Uri.{Authority, RegName}
import org.http4s.client.Client
import org.http4s.{Header, Request, Uri}
import org.log4s.getLogger
import org.typelevel.ci.CIString

class OneFrameNoCaching[F[_]: Concurrent](
                                     client: Client[F],
                                     oneFrameHost: String,
                                     oneFramePort: Int,
                                     oneFrameToken: String
                                   ) extends NoCachingAlgebra[F] {

  private val logger = getLogger

  override def get(pairs: List[Rate.Pair]): F[List[GetOneFrameApiResponse]] = {
    val oneFrameAuthority = Some(Authority(host = RegName(oneFrameHost), port = Some(oneFramePort)))
    val oneFrameUri = Uri(authority = oneFrameAuthority) / "rates" ++? ("pair" -> pairs.map(_.show))
    val request = Request[F](uri = oneFrameUri)
      .putHeaders(Header.Raw(CIString("token"), oneFrameToken))
    logger.info(s"One-frame request: $request")

    for {
      response <- client.expect[List[GetOneFrameApiResponse]](request)
    } yield {
      logger.info(s"One-frame response: $response")
      response
    }
  }
}
