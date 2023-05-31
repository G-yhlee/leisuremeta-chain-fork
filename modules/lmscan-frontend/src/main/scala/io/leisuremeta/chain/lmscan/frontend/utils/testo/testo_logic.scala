package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.Log.*
import scala.util.chaining.*

object TestoLogic:
  val target_1 = Some("123123123")
  val target_2 = Some(123123123)
  val target_3 = Some(123123123.toLong)
  val target_4 = Some(BigDecimal(123123123L))
  val target_5 = Some(BigDecimal(123123123))
