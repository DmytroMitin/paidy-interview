package forex

import cats.effect.{ContextShift, IO, Timer}
import cats.syntax.applicative._
import forex.config.{ApplicationConfig, HttpConfig, OneFrameConfig}
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.http.rates.Protocol
import forex.http.rates.Protocol.{GetApiResponse, GetOneFrameApiResponse}
import forex.services.rates.NoCachingAlgebra
import org.http4s
import org.http4s.{EntityDecoder, Request, Response}
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SpecHelpers(val time: Timestamp) extends AnyFlatSpec with should.Matchers {

  val ec = ExecutionContext.global
  implicit val timer: Timer[IO] = IO.timer(ec)
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)

  def check[A: EntityDecoder[IO, *]](actual: IO[Response[IO]],
                                     expectedStatus: http4s.Status,
                                     expectedBody: Option[A]): Boolean = {
    val actualResp = actual.unsafeRunSync()
    val statusCheck = actualResp.status == expectedStatus
    val bodyCheck =
      expectedBody.fold[Boolean](
        actualResp.body.compile.toVector.unsafeRunSync().isEmpty // Verify Response's body is empty.
      )(
        expected => {
          val actual = actualResp.as[A].unsafeRunSync()
          actual == expected
        }
      )
    statusCheck && bodyCheck
  }

  val config = ApplicationConfig(
    HttpConfig("0.0.0.0", 8081, 40.seconds),
    OneFrameConfig("0.0.0.0", 8080, "1234567890")
  )

  val getOneFrameApiResponse1 = GetOneFrameApiResponse(
    from = Currency.USD,
    to = Currency.JPY,
    bid = Price(1: Integer),
    ask = Price(1: Integer),
    price = Price(1: Integer),
    time_stamp = time
  )
  val getOneFrameApiResponse2 = GetOneFrameApiResponse(
    from = Currency.USD,
    to = Currency.JPY,
    bid = Price(2: Integer),
    ask = Price(2: Integer),
    price = Price(2: Integer),
    time_stamp = time
  )

  var getOneFrameApiResponse = getOneFrameApiResponse1

  val noCaching = new NoCachingAlgebra[IO] {
    override def get(pair: Rate.Pair): IO[List[Protocol.GetOneFrameApiResponse]] =
      List(getOneFrameApiResponse).pure[IO]
  }

  val response: IO[Response[IO]] = new Module[IO](config, noCaching).http.orNotFound.run(
    Request(uri = uri"http://localhost:8081/rates?from=USD&to=JPY")
  )

  val expected = GetApiResponse(
    from = Currency.USD,
    to = Currency.JPY,
    price = Price(1: Integer),
    timestamp = time
  )
  val expected1 = GetApiResponse(
    from = Currency.USD,
    to = Currency.JPY,
    price = Price(2: Integer),
    timestamp = time
  )
}
