package forex

import forex.domain.Timestamp
import forex.http.rates.Protocol.GetApiResponse
import org.http4s

class ActualCacheSpec extends SpecHelpers(Timestamp.now) {

  "Caching service" should "firstly return rate from One-Frame" in {
    check[GetApiResponse](response, http4s.Status.Ok, Some(expected)) should be(true)
  }

  "Caching service" should "return rate from cache" in {
    getOneFrameApiResponse = getOneFrameApiResponse2

    check[GetApiResponse](response, http4s.Status.Ok, Some(expected)) should be(true)
  }
}
