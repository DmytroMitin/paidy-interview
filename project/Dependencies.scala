import sbt._

object Dependencies {

  object Versions {
    val cats                = "2.10.0"
    val catsEffect          = "3.5.2"
    val fs2                 = "3.9.3"
    val http4s              = "0.23.24"
    val circe               = "0.14.6"
    val circeGenericExt     = "0.14.3"
    val pureConfig          = "0.17.4"

    val kindProjector       = "0.13.2"
    val logback             = "1.4.14" // Java 11+
    val scalaCheck          = "1.17.0"
    val scalaTest           = "3.2.17"
    val catsScalaCheck      = "0.3.2"
  }

  object Libraries {
    def circe(artifact: String): ModuleID = {
      val version = artifact match {
        case "circe-generic-extras" => Versions.circeGenericExt
        case _                      => Versions.circe
      }
      "io.circe" %% artifact % version
    }
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s

    lazy val cats                = "org.typelevel"         %% "cats-core"       % Versions.cats
    lazy val catsEffect          = "org.typelevel"         %% "cats-effect"     % Versions.catsEffect
    lazy val fs2                 = "co.fs2"                %% "fs2-core"        % Versions.fs2

    lazy val http4sDsl           = http4s("http4s-dsl")
    lazy val http4sClient        = http4s("http4s-ember-client")
    lazy val http4sServer        = http4s("http4s-ember-server")
    lazy val http4sCirce         = http4s("http4s-circe")
    lazy val circeCore           = circe("circe-core")
    lazy val circeGeneric        = circe("circe-generic")
    lazy val circeGenericExt     = circe("circe-generic-extras")
    lazy val circeParser         = circe("circe-parser")
    lazy val pureConfig          = "com.github.pureconfig" %% "pureconfig"      % Versions.pureConfig

    // Compiler plugins
    lazy val kindProjector       = "org.typelevel"         %% "kind-projector"  % Versions.kindProjector cross CrossVersion.full

    // Runtime
    lazy val logback             = "ch.qos.logback"        %  "logback-classic" % Versions.logback

    // Test
    lazy val scalaTest           = "org.scalatest"         %% "scalatest"       % Versions.scalaTest
    lazy val scalaCheck          = "org.scalacheck"        %% "scalacheck"      % Versions.scalaCheck
    lazy val catsScalaCheck      = "io.chrisdavenport"     %% "cats-scalacheck" % Versions.catsScalaCheck
  }

}
