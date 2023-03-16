package io.leisuremeta.chain.lmscan.frontend
import Dom.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import cats.effect.IO
import tyrian.Html.*
import scala.scalajs.js
import tyrian.*
import Log.log
import ValidPageName.*
import V.*
import org.scalajs.dom.window
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    case PageMsg.PreUpdate(search: PageName, pushHistory: Boolean) =>
      if pushHistory == true then
        window.history.pushState(
          // save page to history
          search.toString(),
          null,
          // show url
          s"${window.location.origin}/"
            ++
              search
                .toString()
                .replace("Detail", "")
                .replace("(", "/")
                .replace(")", "")
                .toLowerCase(),
        )
      (
        model.copy(
          prevPage = model.curPage match
            case PageName.NoPage => model.prevPage
            case _               => model.curPage
          ,
          searchValueStore = search.toString(),
          pageNameStore = getPage(search),
          urlStore = search.toString(),
        ), {
          getPage(search) match
            case PageName.DashBoard =>
              Cmd.Batch(
                OnDataProcess.getData(
                  PageName.Transactions(1),
                ),
                OnDataProcess.getData(
                  PageName.Blocks(1),
                ),
                OnDataProcess.getData(PageName.DashBoard),
                Cmd.Emit(PageMsg.PageUpdate),
              )

            case PageName.Blocks(_) =>
              Cmd.Emit(PageMsg.PageUpdate)

            case PageName.Transactions(_) =>
              Cmd.Emit(PageMsg.PageUpdate)

            case _ =>
              OnDataProcess.getData(
                search,
              )
        },
      )

    // #data update
    case PageMsg.DataUpdate(data: String, page: PageName) =>
      page match
        case PageName.DashBoard =>
          (
            model.copy(apiData = Some(data)),
            Cmd.None,
          )
        case PageName.Transactions(_) =>
          // TODO :: txData , tx_TotalPage 를 init 단계에서 실행되게 하는게 더 나은방법인지 생각해보자
          var updated_tx_TotalPage = 1

          // TODO :: more simple code
          TxParser
            .decodeParser(data)
            .map(data => updated_tx_TotalPage = data.totalPages)

          (
            model.copy(
              txListData = Some(data),
              tx_TotalPage = updated_tx_TotalPage,
            ),
            Cmd.None,
          )

        case PageName.Blocks(_) =>
          // TODO :: txData , tx_TotalPage 를 init 단계에서 실행되게 하는게 더 나은방법인지 생각해보자
          var updated_block_TotalPage          = 1
          var latestBlockList: List[BlockInfo] = List(new BlockInfo)
          // var latestBlockNumber: Int       = 1
          var latestBlockNumber: Long = 1

          BlockParser
            .decodeParser(data)
            .map(data =>
              updated_block_TotalPage = data.totalPages
              latestBlockList = data.payload
                .asInstanceOf[List[BlockInfo]],
            )

          latestBlockNumber =
            getOptionValue(latestBlockList(0).number, 1).asInstanceOf[Long]

          (
            model.copy(
              blockListData = Some(data),
              block_TotalPage = updated_block_TotalPage,
              latestBlockNumber = latestBlockNumber.toString(),
            ),
            Cmd.None,
          )
        case PageName.BlockDetail(_) =>
          (
            model.copy(
              blockDetailData = Some(data),
            ),
            Cmd.emit(PageMsg.PageUpdate),
          )
        case PageName.AccountDetail(_) =>
          (
            model.copy(
              accountDetailData = Some(data),
            ),
            Cmd.emit(PageMsg.PageUpdate),
          )
        case PageName.NftDetail(_) =>
          (
            model.copy(
              nftDetailData = Some(data),
            ),
            Cmd.emit(PageMsg.PageUpdate),
          )
        case PageName.TransactionDetail(_) =>
          (
            model.copy(
              txDetailData = Some(data),
              pageNameStore = page,
            ),
            Cmd.emit(PageMsg.PageUpdate),
          )

        case _ =>
          log(page)
          (
            model,
            Cmd.emit(PageMsg.GetError("페이지를 찾을수 없다..", page)),
          )

    // #page update
    case PageMsg.PageUpdate =>
      (
        model.copy(curPage = model.pageNameStore),
        Cmd.emit(PageMsg.PostUpdate),
      )

    case PageMsg.GetError(msg, page) =>
      page match
        case PageName.Page64(hash) =>
          log(s"트랙잭션 먼저 검색 후 실패시 블록 디테일로 검색한다")
          (
            model,
            Cmd.emit(PageMsg.PreUpdate(PageName.BlockDetail(hash))),
          )
        case _ =>
          page match

            case PageName.DashBoard =>
              dom.document
                .querySelector("#loader-container")
                .asInstanceOf[HTMLElement]
                .style
                .display = "none"
              log(page.toString() + " -> " + msg)
              (
                model,
                Cmd.None,
              )
            case _ =>
              dom.document
                .querySelector("#loader-container")
                .asInstanceOf[HTMLElement]
                .style
                .display = "none"
              log(page.toString() + " -> " + msg)
              (
                model.copy(curPage = PageName.NoPage),
                Cmd.emit(PageMsg.PostUpdate),
              )

    case PageMsg.PostUpdate =>
      (
        model.copy(searchValue = ""),
        Cmd.None,
      )
