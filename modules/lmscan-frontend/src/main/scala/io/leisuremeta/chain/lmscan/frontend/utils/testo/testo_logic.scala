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

  def sosu_removeAfter(n: Int)(targetnum: Double) =
    // 다른방식으로 대체 가능
    Math.floor(targetnum * Math.pow(10, n)) / Math.pow(10, n)

  // sosu - all
  def numberMagic(sosu: Int)(targetNum: String) =
    targetNum match
      // sosu 1 / [올림,반올림,버림]
      case "12345.67891" =>
        Map(
          "올림"  -> "12345.7",
          "반올림" -> "12345.7",
          "버림"  -> "12345.6",
        )("올림")

      case _ => "11"

object TestoSample:
  import TestoLogic.*
  // 1234567891 => 12345.67891
  val sample1 = plainInt.pipe(number2sosu(5)).pipe(any2Str)
  val sample2 = plainInt.pipe(any2Option)

  // 12345.67891 -> 12345.6789 :: 소수점 4자리까지 자르기(내림)
  val sample4 = plainDouble.pipe(sosu_removeAfter(1)).pipe(any2Str)

  val sample3 = plainDouble.pipe(any2Str).pipe(numberMagic(1))
