package forex

import cats.effect.concurrent.{MVar, MVar2}
import cats.effect.{ContextShift, IO, Timer}
import cats.syntax.applicative._
import forex.config.{ApplicationConfig, HttpConfig, OneFrameConfig}
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.http.rates.Protocol.{GetApiResponse, GetOneFrameApiResponse}
import forex.services.rates.NoCachingAlgebra
import org.http4s
import org.http4s.{EntityDecoder, Request, Response}
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class OneFrameSpec extends AnyFlatSpec with should.Matchers {

  val ec = ExecutionContext.global
  implicit val timer: Timer[IO] = IO.timer(ec)
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)

  def check[A: EntityDecoder[IO, *]](actualRespIO: IO[Response[IO]],
                                     expectedStatus: http4s.Status,
                                     expectedBody: Option[A]): IO[Boolean] =
    for {
      actualResp <- actualRespIO
      statusCheck = actualResp.status == expectedStatus
      vec <- actualResp.body.compile.toVector
      actual <- actualResp.as[A]
      bodyCheck =
        expectedBody.fold[Boolean](
          vec.isEmpty // Verify Response's body is empty.
        )(expected =>
          actual == expected
        )
    } yield statusCheck && bodyCheck

  val config = ApplicationConfig(
    HttpConfig("0.0.0.0", 8081, 40.seconds),
    OneFrameConfig("0.0.0.0", 8080, "1234567890")
  )

  def getOneFrameApiResponse1(timestamp: Timestamp): GetOneFrameApiResponse = GetOneFrameApiResponse(
    from = Currency.USD,
    to = Currency.JPY,
    bid = Price(1: Integer),
    ask = Price(1: Integer),
    price = Price(1: Integer),
    time_stamp = timestamp
  )
  def getOneFrameApiResponse2(timestamp: Timestamp): GetOneFrameApiResponse = GetOneFrameApiResponse(
    from = Currency.USD,
    to = Currency.JPY,
    bid = Price(2: Integer),
    ask = Price(2: Integer),
    price = Price(2: Integer),
    time_stamp = timestamp
  )

  def noCaching(getOneFrameApiResponse: GetOneFrameApiResponse): NoCachingAlgebra[IO] =
    _ => List(getOneFrameApiResponse).pure[IO]

  def responseIO(noCaching: NoCachingAlgebra[IO], cache: MVar2[IO, Map[Rate.Pair, Rate]]): IO[Response[IO]] =
    new Module[IO](config, noCaching, cache)
      .http
      .orNotFound
      .run(
        Request(uri = uri"http://localhost:8081/rates?from=USD&to=JPY")
      )

  def expected1(timestamp: Timestamp): GetApiResponse = GetApiResponse(
    from = Currency.USD,
    to = Currency.JPY,
    price = Price(1: Integer),
    timestamp = timestamp
  )
  def expected2(timestamp: Timestamp): GetApiResponse = GetApiResponse(
    from = Currency.USD,
    to = Currency.JPY,
    price = Price(2: Integer),
    timestamp = timestamp
  )

  def test(
            expected1: Timestamp => GetApiResponse,
            expected2: Timestamp => GetApiResponse,
            timestamp: Timestamp
          ): Boolean =
    (for {
      cache <- MVar.of[IO, Map[Rate.Pair, Rate]](Map.empty)
      noCaching1 = noCaching(getOneFrameApiResponse1(timestamp))
      response1 = responseIO(noCaching1, cache)
      res1 <- check[GetApiResponse](response1, http4s.Status.Ok, Some(expected1(timestamp)))
      noCaching2 = noCaching(getOneFrameApiResponse2(timestamp))
      response2 = responseIO(noCaching2, cache)
      res2 <- check[GetApiResponse](response2, http4s.Status.Ok, Some(expected2(timestamp)))
    } yield res1 && res2).unsafeRunSync()

  "Caching service with actual cache" should "firstly return rate from One-Frame, secondly return rate from cache" in {
    test(expected1, expected1, Timestamp.now) should be(true)
  }

  "Caching service with outdated cache" should "return rate from One-Frame both times" in {
    test(expected1, expected2, Timestamp.nowMinus5Minutes) should be(true)
  }
}
