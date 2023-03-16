package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.frontend.Log.log

enum PageName:
  case DashBoard
  case Blocks(page: Int)               extends PageName
  case Transactions(page: Int)         extends PageName
  case TransactionDetail(hash: String) extends PageName
  case BlockDetail(hash: String)       extends PageName
  case NftDetail(hash: String)         extends PageName
  case AccountDetail(hash: String)     extends PageName
  case Page64(hash: String)            extends PageName
  case NoPage                          extends PageName

object ValidPageName:
  def getPage(search: PageName): PageName =
    search match
      case PageName.DashBoard            => search
      case PageName.Blocks(_)            => search
      case PageName.Transactions(_)      => search
      case PageName.BlockDetail(_)       => search
      case PageName.AccountDetail(_)     => search
      case PageName.TransactionDetail(_) => search
      case PageName.NftDetail(_)         => search
      case _ =>
        search.toString().length() match
          case 40 => PageName.AccountDetail(search.toString())
          case 25 => PageName.NftDetail(search.toString())
          case 64 => PageName.Page64(search.toString())
          case _ =>
            PageName.NoPage

  def getPageString(search: String): PageName =
    search.toString().length() match
      case 40 => PageName.AccountDetail(search.toString())
      case 25 => PageName.NftDetail(search.toString())
      case 64 => PageName.Page64(search.toString())
      case _  => PageName.NoPage

  def getPageFromStr(search: String): PageName =
    search match
      case "DashBoard"                 => PageName.DashBoard
      case s"Blocks($page)"            => PageName.Blocks(page.toInt)
      case s"Transactions($page)"      => PageName.Transactions(page.toInt)
      case s"BlockDetail($hash)"       => PageName.BlockDetail(hash)
      case s"AccountDetail($hash)"     => PageName.AccountDetail(hash)
      case s"TransactionDetail($hash)" => PageName.TransactionDetail(hash)
      case s"NftDetail($hash)"         => PageName.NftDetail(hash)
      case _ =>
        search.length() match
          case 40 => PageName.AccountDetail(search)
          case 25 => PageName.NftDetail(search)
          case 64 => PageName.Page64(search)
          case _  => PageName.NoPage
