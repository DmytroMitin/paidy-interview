package forex

import cats.effect._
import com.comcast.ip4s.{Host, IpLiteralSyntax, Port}
import forex.config._
import forex.domain.Rate
import forex.services.rates.interpreters.OneFrameNoCaching
import fs2.io.net.Network
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder

object Main extends IOApp.Simple {

  override val run: IO[Unit] =
    new Application[IO].resource.useForever

}

class Application[F[_]: Async: Network] {

  val resource: Resource[F, Unit] =
    for {
      config <- Config.resource("app")
      client <- EmberClientBuilder.default[F].build
      noCaching = new OneFrameNoCaching[F](client, config.oneFrame.host, config.oneFrame.port, config.oneFrame.token)
      deferred <- Resource.eval(Deferred[F, Map[Rate.Pair, Rate]])
      _ <- Resource.eval(deferred.complete(Map.empty))
      cache <- Resource.eval(Ref.of(deferred))
      module = new Module[F](config, noCaching, cache)
      _ <- EmberServerBuilder.default[F]
            .withHost(Host.fromString(config.http.host).getOrElse(ipv4"0.0.0.0"))
            .withPort(Port.fromInt(config.http.port).getOrElse(port"8080"))
            .withHttpApp(module.httpApp)
            .build
    } yield ()

}
