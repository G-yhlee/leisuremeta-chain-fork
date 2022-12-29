val V = new {
  val Scala      = "3.2.1"
  val ScalaGroup = "3.2"

  val catsEffect = "3.4.3"
  val tapir      = "1.2.4"
  val sttp       = "3.8.5"
  val circe      = "0.15.0-M1"
  val refined    = "0.9.28"
  val scodecBits = "1.1.30"
  val shapeless  = "3.2.0"
  val fs2        = "3.2.7"

  val typesafeConfig = "1.4.2"
  val bouncycastle   = "1.70"
  val sway           = "0.16.2"
  val jasync = "2.1.8"

  val okhttp3LoggingInterceptor = "4.10.0"

  val web3J = "5.0.0"

  val scribe          = "3.10.5"
  val hedgehog        = "0.9.0"
  val organiseImports = "0.6.0"

  val scalaJavaTime = "2.3.0"
  val jsSha3        = "0.8.0"
  val elliptic      = "6.5.4"
  val typesElliptic = "6.4.12"
}

val Dependencies = new {

  lazy val node = Seq(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-armeria-server-cats" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"          % V.tapir,
      "com.outr"                    %% "scribe-slf4j"              % V.scribe,
      "com.typesafe" % "config" % V.typesafeConfig,
      ("io.swaydb"  %% "swaydb" % V.sway).cross(CrossVersion.for3Use2_13),
    ),
    excludeDependencies ++= Seq(
      "org.scala-lang.modules" % "scala-collection-compat_2.13",
      "org.scala-lang.modules" % "scala-java8-compat_2.13",
    ),
  )

  lazy val ethGateway = Seq(
    libraryDependencies ++= Seq(
      "com.outr"    %% "scribe-slf4j" % V.scribe,
      "com.typesafe" % "config"       % V.typesafeConfig,
      "org.web3j"    % "core"         % V.web3J,
      "com.squareup.okhttp3" % "logging-interceptor" % V.okhttp3LoggingInterceptor,
      "com.github.jasync-sql" % "jasync-mysql" % V.jasync,
    ),
  )

  lazy val archieve = Seq(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "armeria-backend-cats" % V.sttp,
      "com.outr"    %% "scribe-slf4j" % V.scribe,
      "com.typesafe" % "config"       % V.typesafeConfig,
    ),
  )

  lazy val api = Seq(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "shapeless3-deriving" % V.shapeless,
      "org.typelevel" %% "cats-effect"         % V.catsEffect,
      "com.softwaremill.sttp.tapir"   %% "tapir-armeria-server-cats" % V.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"          % V.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle"   % V.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui"          % V.tapir,
      "com.softwaremill.sttp.client3" %% "core"                      % V.sttp,
    ),
  )

  lazy val lib = Seq(
    libraryDependencies ++= Seq(
      "io.circe"      %%% "circe-generic"       % V.circe,
      "io.circe"      %%% "circe-parser"        % V.circe,
      "io.circe"      %%% "circe-refined"       % V.circe,
      "eu.timepit"    %%% "refined"             % V.refined,
      "org.scodec"    %%% "scodec-bits"         % V.scodecBits,
      "org.typelevel" %%% "shapeless3-deriving" % V.shapeless,
      "org.typelevel" %%% "shapeless3-typeable" % V.shapeless,
      "co.fs2"        %%% "fs2-core"            % V.fs2,
    ),
  )

  lazy val libJVM = Seq(
    libraryDependencies ++= Seq(
      "org.bouncycastle" % "bcprov-jdk15on" % V.bouncycastle,
      "com.outr"        %% "scribe-slf4j"   % V.scribe,
    ),
  )

  lazy val libJS = Seq(
    libraryDependencies ++= Seq(
      "com.outr" %%% "scribe" % V.scribe,
    ),
    Compile / npmDependencies ++= Seq(
      "js-sha3"         -> V.jsSha3,
      "elliptic"        -> V.elliptic,
      "@types/elliptic" -> V.typesElliptic,
    ),
  )

  lazy val tests = Def.settings(
    libraryDependencies ++= Seq(
      "qa.hedgehog" %%% "hedgehog-munit" % V.hedgehog % Test,
    ),
    Test / fork := true,
  )
}

ThisBuild / organization := "org.leisuremeta"
ThisBuild / version      := "0.0.1-SNAPSHOT"
ThisBuild / scalaVersion := V.Scala
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % V.organiseImports
ThisBuild / semanticdbEnabled := true

lazy val root = (project in file("."))
  .aggregate(node, api.jvm, api.js, lib.jvm, lib.js, ethGateway, ethGatewayWithdraw)

lazy val node = (project in file("modules/node"))
  .settings(Dependencies.node)
  .settings(Dependencies.tests)
  .settings(
    name := "leisuremeta-chain-node",
    assemblyMergeStrategy := {
      case x if x `contains` "io.netty.versions.properties" =>
        MergeStrategy.first
      case x if x `contains` "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
  )
  .dependsOn(api.jvm)

lazy val ethGateway = (project in file("modules/eth-gateway"))
  .settings(Dependencies.ethGateway)
  .settings(Dependencies.tests)
  .settings(
    name := "leisuremeta-chain-eth-gateway-deposit",
    assemblyMergeStrategy := {
      case x if x `contains` "io.netty.versions.properties" =>
        MergeStrategy.first
      case x if x `contains` "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
  )
  .dependsOn(api.jvm)

lazy val ethGatewayWithdraw = (project in file("modules/eth-gateway-withdraw"))
  .settings(Dependencies.ethGateway)
  .settings(Dependencies.tests)
  .settings(
    name := "leisuremeta-chain-eth-gateway-withdraw",
    assemblyMergeStrategy := {
      case x if x `contains` "io.netty.versions.properties" =>
        MergeStrategy.first
      case x if x `contains` "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
  )
  .dependsOn(api.jvm)

lazy val archieve = (project in file ("modules/archieve"))
  .settings(Dependencies.archieve)
  .settings(Dependencies.tests)
  .settings(
    name := "leisuremeta-chain-archieve",
    assemblyMergeStrategy := {
      case x if x `contains` "io.netty.versions.properties" =>
        MergeStrategy.first
      case x if x `contains` "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
  )
  .dependsOn(api.jvm)

lazy val api = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/api"))
  .settings(Dependencies.api)
  .settings(
    scalacOptions ++= Seq(
      "-Xmax-inlines:64",
    ),
    Compile / compile / wartremoverErrors ++= Warts.allBut(Wart.NoNeedImport),
  )
  .dependsOn(lib)

lazy val lib = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("modules/lib"))
  .settings(Dependencies.lib)
  .settings(Dependencies.tests)
  .jvmSettings(Dependencies.libJVM)
  .jsSettings(Dependencies.libJS)
  .jsSettings(
    useYarn := true,
    Test / scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    },
    scalacOptions ++= Seq(
      "-scalajs",
    ),
    Test / fork := false,
    Compile / compile / wartremoverErrors ++= Warts.all,
  )
  .jsConfigure { project =>
    project
      .enablePlugins(ScalaJSBundlerPlugin)
      .enablePlugins(ScalablyTypedConverterPlugin)
  }
