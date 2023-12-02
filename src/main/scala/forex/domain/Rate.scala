package forex.domain

import cats.Show
import cats.syntax.show._

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )
  object Pair {
    val values: List[Pair] = for {
      currency  <- Currency.values
      currency1 <- Currency.values
      if currency != currency1
    } yield Pair(currency, currency1)

    implicit val show: Show[Pair] = Show.show(pair => pair.from.show + pair.to.show)
  }
}
