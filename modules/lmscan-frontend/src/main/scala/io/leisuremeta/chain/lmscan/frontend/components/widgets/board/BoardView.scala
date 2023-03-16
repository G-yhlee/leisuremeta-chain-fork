package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model.SummaryModel

object Board:
  val LM_Price     = "LM PRICE"
  val Block_Number = "BLOCK NUMBER"
  val Transactions = "24H TRANSACTIONS"
  val Accounts     = "TOTAL ACCOUNTS"

object BoardView:
  def view(model: Model): Html[Msg] =
    val data: SummaryModel =
      ApiParser.decodeParser(model.apiData.get).getOrElse(new SummaryModel)

    div(`class` := "board-area")(
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-grey")(Board.LM_Price),
            div()(
              plainStr(data.lmPrice).take(6) + " USDT",
            ),
          ),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-grey")(Board.Block_Number),
            div()(model.latestBlockNumber),
          ),
        ),
      ),
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-grey")(Board.Transactions),
            div()(
              // getOptionValue(data.txCountInLatest24h, "-").toString(),
              // String.format(
              //   "%,d",
              //   getOptionValue(data.txCountInLatest24h, "-").toString().toDouble,
              // ),
              plainStr(data.txCountInLatest24h),
            ),
          ),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-grey")(Board.Accounts),
            div()(
              plainStr(data.totalAccounts),
              // "39,104",
            ),
          ),
        ),
      ),
    )
