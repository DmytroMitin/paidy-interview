package forex

import cats.effect.concurrent.{MVar, MVar2}
import cats.effect.{ContextShift, IO, Timer}
import cats.syntax.applicative._
import forex.config.{ApplicationConfig, HttpConfig, OneFrameConfig}
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.http.rates.Protocol.{GetApiResponse, GetOneFrameApiResponse}
import forex.services.rates.NoCachingAlgebra
import org.http4s
import org.http4s.{EntityDecoder, Query, Request, Response, Uri}
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

  def getOneFrameApiResponse(
                              from: Currency,
                              to: Currency,
                              price: Int,
                              timestamp: Timestamp
                            ): GetOneFrameApiResponse =
    GetOneFrameApiResponse(
      from = from,
      to = to,
      bid = Price(price: Integer),
      ask = Price(price: Integer),
      price = Price(price: Integer),
      time_stamp = timestamp
    )

  def getOneFrameApiResponse1UsdJpy(timestamp: Timestamp): GetOneFrameApiResponse =
    getOneFrameApiResponse(Currency.USD, Currency.JPY, 1, timestamp)
  def getOneFrameApiResponse2UsdJpy(timestamp: Timestamp): GetOneFrameApiResponse =
    getOneFrameApiResponse(Currency.USD, Currency.JPY, 2, timestamp)
  def getOneFrameApiResponse1UsdEur(timestamp: Timestamp): GetOneFrameApiResponse =
    getOneFrameApiResponse(Currency.USD, Currency.EUR, 3, timestamp)
  def getOneFrameApiResponse2UsdEur(timestamp: Timestamp): GetOneFrameApiResponse =
    getOneFrameApiResponse(Currency.USD, Currency.EUR, 4, timestamp)

  def noCaching(getOneFrameApiResponses: List[GetOneFrameApiResponse]): NoCachingAlgebra[IO] =
    _ => getOneFrameApiResponses.pure[IO]

  def responseIO(
                  from: String,
                  to: String,
                  noCaching: NoCachingAlgebra[IO],
                  cache: MVar2[IO, Map[Rate.Pair, Rate]]
                ): IO[Response[IO]] =
    new Module[IO](config, noCaching, cache)
      .http
      .orNotFound
      .run(
        Request(uri = Uri(
          scheme = Some(Uri.Scheme.http),
          authority = Some(Uri.Authority(host = Uri.RegName("localhost"), port = Some(8081))),
          path = "/rates",
          query = Query.fromString(s"from=$from&to=$to")
        ))
      )

  def responseUsdJpyIO(noCaching: NoCachingAlgebra[IO], cache: MVar2[IO, Map[Rate.Pair, Rate]]): IO[Response[IO]] =
    responseIO("USD", "JPY", noCaching, cache)
  def responseUsdEurIO(noCaching: NoCachingAlgebra[IO], cache: MVar2[IO, Map[Rate.Pair, Rate]]): IO[Response[IO]] =
    responseIO("USD", "EUR", noCaching, cache)

  def expected(from: Currency, to: Currency, price: Int, timestamp: Timestamp): GetApiResponse =
    GetApiResponse(
      from = from,
      to = to,
      price = Price(price: Integer),
      timestamp = timestamp
    )

  def expected1UsdJpy(timestamp: Timestamp): GetApiResponse =
    expected(Currency.USD, Currency.JPY, 1, timestamp)
  def expected2UsdJpy(timestamp: Timestamp): GetApiResponse =
    expected(Currency.USD, Currency.JPY, 2, timestamp)
  def expected1UsdEur(timestamp: Timestamp): GetApiResponse =
    expected(Currency.USD, Currency.EUR, 3, timestamp)
  def expected2UsdEur(timestamp: Timestamp): GetApiResponse =
    expected(Currency.USD, Currency.EUR, 4, timestamp)

  def test(
            expected1UsdJpy: Timestamp => GetApiResponse,
            expected2UsdJpy: Timestamp => GetApiResponse,
            expected1UsdEur: Timestamp => GetApiResponse,
            expected2UsdEur: Timestamp => GetApiResponse,
            timestamp: Timestamp
          ): Boolean =
    (for {
      cache <- MVar.of[IO, Map[Rate.Pair, Rate]](Map.empty)
      noCaching1 = noCaching(List(getOneFrameApiResponse1UsdJpy(timestamp), getOneFrameApiResponse1UsdEur(timestamp)))
      response1UsdJpy = responseUsdJpyIO(noCaching1, cache)
      response1UsdEur = responseUsdEurIO(noCaching1, cache)
      res1UsdJpy <- check[GetApiResponse](response1UsdJpy, http4s.Status.Ok, Some(expected1UsdJpy(timestamp)))
      res1UsdEur <- check[GetApiResponse](response1UsdEur, http4s.Status.Ok, Some(expected1UsdEur(timestamp)))
      noCaching2 = noCaching(List(getOneFrameApiResponse2UsdJpy(timestamp), getOneFrameApiResponse2UsdEur(timestamp)))
      response2UsdJpy = responseUsdJpyIO(noCaching2, cache)
      response2UsdEur = responseUsdEurIO(noCaching2, cache)
      res2UsdJpy <- check[GetApiResponse](response2UsdJpy, http4s.Status.Ok, Some(expected2UsdJpy(timestamp)))
      res2UsdEur <- check[GetApiResponse](response2UsdEur, http4s.Status.Ok, Some(expected2UsdEur(timestamp)))
    } yield res1UsdJpy && res2UsdJpy && res1UsdEur && res2UsdEur).unsafeRunSync()

  "Caching service with actual cache" should "firstly return rates from One-Frame, secondly return rates from cache" in {
    test(expected1UsdJpy, expected1UsdJpy, expected1UsdEur, expected1UsdEur, Timestamp.now) should be(true)
  }

  "Caching service with outdated cache" should "return rates from One-Frame both times" in {
    test(expected1UsdJpy, expected2UsdJpy, expected1UsdEur, expected2UsdEur, Timestamp.nowMinus5Minutes) should be(true)
  }
}
