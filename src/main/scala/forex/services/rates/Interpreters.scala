package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import interpreters._
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def actual[F[_]: Sync](client: Client[F]): Algebra[F] = new OneFrame[F](client)
}
