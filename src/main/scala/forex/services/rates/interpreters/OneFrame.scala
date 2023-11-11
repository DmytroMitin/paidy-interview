package forex.services.rates.interpreters

import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.show._
import forex.domain.{Rate, Timestamp}
import forex.http.rates.Protocol.GetOneFrameApiResponse
import forex.services.rates.Algebra
import forex.services.rates.errors._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.{Header, Request}

class OneFrame[F[_]: Sync](client: Client[F]) extends Algebra[F] {

  // TODO make domain, port, token configurable
  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    val request = Request[F](
      uri = uri"http://localhost:8080/rates" +? ("pair", pair.from.show + pair.to.show)
    ).putHeaders(Header("token", "10dc303535874aeccc86a8251e6992f5"))
    println(s"request=$request")

    for {
      response <- client.expect[List[GetOneFrameApiResponse]](request)
    } yield {
      println(s"response=$response")
      Rate(pair, response.head.price, Timestamp.now).asRight[Error]
    }
  }
}
