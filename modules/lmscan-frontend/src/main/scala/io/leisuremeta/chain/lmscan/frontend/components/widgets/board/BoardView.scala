package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model.SummaryModel

object Board:
  val LM_Price     = "LM PRICE"
  val Block_Number = "BLOCK NUMBER"
  val Transactions = "TOTAL TRANSACTIONS"
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
            div(`class` := "font-16px color-white font-bold")(Board.LM_Price),
            div(`class` := "color-white font-bold")(
              plainStr(data.lmPrice).take(6) + " USDT",
            ),
          ),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold color-white")(
              Board.Block_Number,
            ),
            div(`class` := "color-white font-bold")(model.latestBlockNumber),
          ),
        ),
      ),
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold color-white")(
              Board.Transactions,
            ),
            div(`class` := "color-white font-bold")(
              String
                .format(
                  "%.0f",
                  plainLong(data.totalTxSize).toDouble / Math
                    .pow(10, 3)
                    .toDouble,
                ) + " GB",
            ),
          ),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold color-white")(
              Board.Accounts,
            ),
            div(`class` := "color-white font-bold")(
              plainStr(data.totalAccounts),
              // "39,104",
            ),
          ),
        ),
      ),
    )
