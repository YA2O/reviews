val Http4sVersion = "0.23.1"
val CirceVersion = "0.14.1"
val RefinedVersion = "0.9.27"
val CirisVersion = "2.1.1"
val ScalaCheckVersion = "1.14.1"
val LogbackVersion = "1.2.5"
val MunitVersion = "0.7.29"
val MunitCatsEffectVersion = "1.0.7"

lazy val root = (project in file("."))
  .settings(
    organization := "bzh.ya2o",
    name := "reviews",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.8",
    libraryDependencies ++= Seq(
      "is.cir" %% "ciris" % CirisVersion,
      "is.cir" %% "ciris-refined" % CirisVersion,
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-ember-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion % Test,
      "io.circe" %% "circe-parser" % CirceVersion,
      "io.circe" %% "circe-refined" % CirceVersion,
      "eu.timepit" %% "refined" % RefinedVersion,
      "eu.timepit" %% "refined-cats" % RefinedVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.scalacheck" %% "scalacheck" % ScalaCheckVersion,
      "org.scalameta" %% "munit" % MunitVersion % Test,
      "org.typelevel" %% "munit-cats-effect-3" % MunitCatsEffectVersion % Test
    ),
    Test / run / fork := true,
    testFrameworks += new TestFramework("munit.Framework")
  )

addCommandAlias("validate", "clean; test; scalafmtCheck; scalafmtSbtCheck; doc")
