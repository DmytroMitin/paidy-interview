package forex

import forex.domain.Timestamp
import forex.http.rates.Protocol.GetApiResponse
import org.http4s

class OutdatedCacheSpec extends SpecHelpers(Timestamp.nowMinus5Minutes) {

  "Caching service with outdated cache" should "firstly return rate from One-Frame" in {
    check[GetApiResponse](response, http4s.Status.Ok, Some(expected)) should be(true)
  }

  "Caching service with outdated cache" should "secondly return rate from One-Frame too" in {
    getOneFrameApiResponse = getOneFrameApiResponse2

    check[GetApiResponse](response, http4s.Status.Ok, Some(expected1)) should be(true)
  }
}
