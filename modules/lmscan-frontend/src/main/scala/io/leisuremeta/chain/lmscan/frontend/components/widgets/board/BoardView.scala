package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.frontend.Log.log

object Board:
  val LM_Price     = "LM PRICE"
  val Block_Number = "BLOCK NUMBER"
  val Transactions = "TOTAL DATA SIZE"
  val Accounts     = "TOTAL ACCOUNTS"

object BoardView:
  def view(model: Model): Html[Msg] =

    log("model.apiData.get")
    log(model.apiData.get)

    val data: SummaryModel =
      ApiParser.decodeParser(model.apiData.get).getOrElse(new SummaryModel)

    log(data)
    log(data.totalTxSize)
    // div()

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
            div(`class` := "color-white font-bold")(
              f"${model.latestBlockNumber.toLong}%,d",
            ),
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
              {
                log("data.totalTxSize")
                log(data.totalTxSize)
                String
                  .format(
                    "%.3f",
                    // plainLong(data.totalTxSize).toDouble / Math
                    plainDouble(
                      data.totalTxSize,
                    ).toDouble / (1024 * 1024).toDouble,
                    // data.totalTxSize.toDouble / (1024 * 1024 * 1024).toDouble,
                  ) + " MB"
              },
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
              f"${plainLong(data.totalAccounts).toLong}%,d",
              // "39,104",
            ),
          ),
        ),
      ),
    )
