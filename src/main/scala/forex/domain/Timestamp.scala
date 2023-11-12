package forex.domain

import java.time.OffsetDateTime

case class Timestamp(value: OffsetDateTime) extends AnyVal {
  def isNotOlderThan5Minutes: Boolean =
    value.isAfter(Timestamp.nowMinus5Minutes.value)
}

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  def nowMinus5Minutes: Timestamp =
    Timestamp(OffsetDateTime.now.minusMinutes(5))
}
