package forex

import cats.effect._
import forex.config._
import forex.services.rates.interpreters.OneFrameNoCaching
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      client <- BlazeClientBuilder[F](ec).stream
      noCaching = new OneFrameNoCaching[F](client, config.oneFrame.host, config.oneFrame.port, config.oneFrame.token)
      module = new Module[F](config, noCaching)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
