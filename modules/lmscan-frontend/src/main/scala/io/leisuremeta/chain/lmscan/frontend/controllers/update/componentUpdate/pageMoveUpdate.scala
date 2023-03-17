package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object PageMoveUpdate:
  val pageMoveCnt = 10

  def update(model: Model): PageMoveMsg => (Model, Cmd[IO, Msg]) =
    case PageMoveMsg.Next =>
      model.curPage match
        case PageName.Transactions(page) =>
          val updated = page + pageMoveCnt
          (
            model.copy(
              tx_list_Search = updated.toString(),
            ),
            Cmd.Batch(
              OnDataProcess.getData(
                PageName.Transactions(updated),
              ),
              Cmd.emit(PageMsg.PreUpdate(PageName.Transactions(updated))),
            ),
          )
        case PageName.Blocks(page) =>
          val updated = page + pageMoveCnt
          (
            model.copy(
              block_list_Search = updated.toString(),
            ),
            Cmd.Batch(
              OnDataProcess.getData(
                PageName.Blocks(updated),
              ),
              Cmd.emit(PageMsg.PreUpdate(PageName.Blocks(updated))),
            ),
          )
        case _ => (model, Cmd.None)

    case PageMoveMsg.Prev =>
      model.curPage match
        case PageName.Transactions(page) =>
          val updated = page - pageMoveCnt
          (
            model.copy(
              tx_list_Search = updated.toString(),
            ),
            Cmd.Batch(
              OnDataProcess.getData(
                PageName.Transactions(updated),
              ),
              Cmd.emit(PageMsg.PreUpdate(PageName.Transactions(updated))),
            ),
          )
        case PageName.Blocks(page) =>
          val updated = page - pageMoveCnt
          (
            model.copy(
              block_list_Search = updated.toString(),
            ),
            Cmd.Batch(
              OnDataProcess.getData(
                PageName.Blocks(updated),
              ),
              Cmd.emit(PageMsg.PreUpdate(PageName.Blocks(updated))),
            ),
          )
        case _ => (model, Cmd.None)

    case PageMoveMsg.Goto(page: PageName) =>
      (
        model.copy(
          curPage = page,
          prevPage = page,
          block_list_Search = page match
            case PageName.Blocks(p) => p.toString()
            case _                  => "11"
          ,
          tx_list_Search = page match
            case PageName.Transactions(p) => p.toString()
            case _                        => "11",
        ),
        OnDataProcess.getData(
          page,
        ),
      )

    case PageMoveMsg.Get(value) =>
      model.curPage match
        case PageName.Transactions(_) =>
          (
            model.copy(tx_list_Search = value),
            Cmd.None,
          )
        case PageName.Blocks(_) =>
          (
            model.copy(block_list_Search = value),
            Cmd.None,
          )
        case _ => (model, Cmd.None)

    case PageMoveMsg.Patch(value) =>
      model.curPage match
        case PageName.Transactions(page) =>
          val str = value match
            case "Enter" => model.tx_list_Search
            case _       => value

          val res = // filter only number like string and filter overflow pagenumber
            !str.forall(
              Character.isDigit,
            ) || str == "" || str.toInt > model.tx_TotalPage match
              case true  => page
              case false => str.toInt

          log(s"PageMoveMsg.Patch ${str} ${res}")
          (
            model.copy(
              tx_list_Search = res.toString(),
            ),
            Cmd.Batch(
              OnDataProcess.getData(
                PageName.Transactions(res),
              ),
              Cmd.emit(PageMsg.PreUpdate(PageName.Transactions(res))),
            ),
          )
        case PageName.Blocks(page) =>
          val str = value match
            case "Enter" => model.block_list_Search
            case _       => value

          val res = // filter only number like string and filter overflow pagenumber
            !str.forall(
              Character.isDigit,
            ) || str == "" || str.toInt > model.block_TotalPage match
              case true  => page
              case false => str.toInt

          (
            model.copy(
              block_list_Search = res.toString(),
            ),
            Cmd.Batch(
              OnDataProcess.getData(
                PageName.Blocks(res),
              ),
              Cmd.emit(PageMsg.PreUpdate(PageName.Blocks(res))),
            ),
          )

        case _ => (model, Cmd.None)
