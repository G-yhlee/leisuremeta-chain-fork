package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Board:
  val LM_Price     = "LM Price"
  val Block_Number = "Block Number"
  val Transactions = "24h Transactions"
  val Accounts     = "Total Accounts"

object BoardView:
  def view(model: Model): Html[Msg] =
    val data: ApiData = ApiParser.decodeParser(model.apiData.get).getOrElse(new ApiData(0,0.0,0,0,0,0))
      
    div(`class` := "board-area")(
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-grey")(Board.LM_Price),
            div()(data.lmPrice.toString() + " USDT"),
          ),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-grey")(Board.Block_Number),
            div()(data.blockNumber.toString()),
          ),
        ),
      ),
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-grey")(Board.Transactions),
            div()(data.txCountInLatest24h.toString()),
          ),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-grey")(Board.Accounts),
            div()(data.totalAccounts.toString()),
          ),
        ),
      ),
    )
