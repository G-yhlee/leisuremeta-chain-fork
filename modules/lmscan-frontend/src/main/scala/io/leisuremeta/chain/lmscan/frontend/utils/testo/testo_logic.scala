package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.Log.*
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.frontend.TestoLogic.plainInt

object TestoLogic:
//     {
//     "id": 9124,
//     "lmPrice": 0.02836384,
//     "blockNumber": 1774948,
//     "totalTxSize": 842923267,
//     "totalAccounts": 230378,
//     "createdAt": 1685515435
// }

  val plainInt    = 1234567891
  val plainDouble = 12345.67891
  val plainLong   = 12345.67891

  def any2Str[T](d: T)    = s"$d"
  def any2Option[T](d: T) = Some(d)

  val strInt    = plainInt.pipe(any2Str)
  val strDouble = plainDouble.pipe(any2Str)
  val strLong   = plainLong.pipe(any2Str)

  def str2Int(d: String)        = d.toInt
  def str2Long(d: String)       = d.toLong
  def str2BigDecimal(d: String) = BigDecimal(d)

  def number2sosu(n: Int)(targetnum: Int) = targetnum / Math.pow(10, n)

object TestoSample:
  import TestoLogic.*
  // 1234567891 => 12345.67891
  val sample1 = plainInt.pipe(number2sosu(5)).pipe(any2Str)
  val sample2 = plainInt.pipe(any2Option)
