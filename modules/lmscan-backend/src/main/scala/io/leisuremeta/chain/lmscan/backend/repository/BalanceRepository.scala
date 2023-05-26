package io.leisuremeta.chain.lmscan.backend.repository
import scala.io.Source
import cats.effect.{Async}
import cats.data.EitherT
import com.typesafe.config.ConfigFactory

val config = ConfigFactory.load()
val url    = config.getString("scan.base")

object BalanceRepository extends CommonQuery:
  import ctx.{*, given}
  def getBalance[F[_]: Async]: EitherT[F, String, Option[String]] =

    val result = Source.fromURL(url).mkString

    EitherT.rightT(Some(result))

  def getBalanceSimple =
    Source.fromURL(url).mkString
