
lazy val noPublishSettings = Seq(
  publishArtifact := false,
  packagedArtifacts := Map.empty,
  publish / skip := true
)

ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.simon"
ThisBuild / organizationName := "simon"

val zioVersion = "2.0.10"

lazy val `scala-libs-root` =
  project
    .in(file("."))
    .settings(noPublishSettings)

lazy val `zio-simple-actor` =
  project
    .in(file("actor"))
    .settings(
      moduleName := "zio-simple-actor",
      name := "zio-simple-actor",
      description := "zio simple actor"
    )
    .settings(
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % zioVersion,
        "dev.zio" %% "zio-test" % zioVersion % "test"
      )
    )
