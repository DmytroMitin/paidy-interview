package forex

package object services {
  type RatesService[F[_]] = rates.CachingAlgebra[F]
  final val RatesServices = rates.Interpreters
}
