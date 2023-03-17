package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import scala.scalajs.js
import Log.*
import org.scalajs.dom.window
import io.leisuremeta.chain.lmscan.frontend.ValidPageName.getPageFromUrl
object Init:
  val page                = PageName.DashBoard
  val toggle              = true
  val toggleTxDetailInput = true
  val tx_CurrentPage      = 1
  val tx_TotalPage        = 1
  val block_CurrentPage   = 1
  val block_TotalPage     = 1
  val block_list_Search   = "1"
  val tx_list_Search      = "1"

  // TODO :: could be list
  val apiCmd: Cmd.Batch[IO, Msg] =
    Cmd.Batch(OnDataProcess.getData(PageName.DashBoard))

  val txCmd: Cmd.Batch[IO, Msg] =
    Cmd.Batch(
      OnDataProcess.getData(
        PageName.Transactions(tx_CurrentPage),
      ),
    )

  val blockCmd: Cmd.Batch[IO, Msg] =
    Cmd.Batch(
      OnDataProcess.getData(
        PageName.Blocks(block_CurrentPage),
      ),
    )

  val path = window.location.pathname

  val path_match = log(
    getPageFromUrl(path) match
      case PageName.NoPage =>
        log("#path_match :: 매칭되는 페이지가 없습니다")
        window.history.pushState(
          null,
          null,
          s"${window.location.origin}",
        )
        apiCmd
      case PageName.Blocks(page) =>
        Cmd.Batch(
          Cmd.Emit(
            PageMsg.PreUpdate(
              getPageFromUrl(path),
            ),
          ),
          Cmd.Emit(
            PageMoveMsg.Goto(
              PageName.Blocks(page),
            ),
          ),
        )
      case PageName.Transactions(page) =>
        Cmd.Batch(
          Cmd.Emit(
            PageMsg.PreUpdate(
              getPageFromUrl(path),
            ),
          ),
          Cmd.Emit(
            PageMoveMsg.Goto(
              PageName.Transactions(page),
            ),
          ),
        )
      case _ =>
        Cmd.Batch(
          Cmd.Emit(
            PageMsg.PreUpdate(
              getPageFromUrl(path),
            ),
          ),
        ),
  )

  val setProtocol =
    if window.location.href
        .contains("http:") && !window.location.href.contains("local")
    then window.location.href = window.location.href.replace("http:", "https:")

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (
      Model(
        page,
        page,
        "",
        toggle,
        toggleTxDetailInput,
        tx_TotalPage,
        block_TotalPage,
        block_list_Search,
        tx_list_Search,
      ),
      path_match ++ txCmd ++ blockCmd,
    )
