package forex.http
package rates

import cats.effect.Sync
import forex.domain.Currency.show
import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class GetApiRequest(
      from: Currency,
      to: Currency
  )

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  implicit val currencyEncoder: Encoder[Currency] =
    Encoder.instance[Currency] { show.show _ andThen Json.fromString }

  implicit val pairEncoder: Encoder[Pair] =
    deriveConfiguredEncoder[Pair]

  implicit val rateEncoder: Encoder[Rate] =
    deriveConfiguredEncoder[Rate]

  implicit val responseEncoder: Encoder[GetApiResponse] =
    deriveConfiguredEncoder[GetApiResponse]

  final case class GetOneFrameApiResponse(
      from: Currency,
      to: Currency,
      bid: Price,
      ask: Price,
      price: Price,
      time_stamp: Timestamp
  )

  implicit val oneFrameResponseDecoder: Decoder[GetOneFrameApiResponse] = deriveConfiguredDecoder

  implicit def oneFrameResponseEntityDecoder[F[_] : Sync]: EntityDecoder[F, List[GetOneFrameApiResponse]] =
    jsonOf

}
