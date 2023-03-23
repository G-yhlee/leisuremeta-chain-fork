package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.Builder.*

object Tables:
  def render(model: Model): Html[Msg] =
    getPage(model) match
      case PageCase.DashBoard(_, _, _, _) =>
        div(`class` := "table-area")(
          div(id := "oop-table-blocks", `class` := "table-list x")(
            BlockTable.view(model),
            TransactionTable.view(model),
          ),
        )

      case PageCase.Blocks(_, _, _, _) =>
        div(`class` := "table-area")(
          div(`class` := "font-40px pt-16px font-block-detail color-white")(
            "Blocks",
          ),
          div(id := "oop-table-blocks", `class` := "table-list x")(
            BlockTable.view(model),
          ),
        )

      case PageCase.Transactions(_, _, _, _) =>
        div(`class` := "table-area")(
          div(`class` := "font-40px pt-16px font-block-detail color-white")(
            "Transactions",
          ),
          div(id := "oop-table-blocks", `class` := "table-list x")(
            TransactionTable.view(model),
          ),
        )

      case PageCase.BlockDetail(_, _, _, _) =>
        div(`class` := "table-area ")(
          div(id := "oop-table-blocks", `class` := "table-list x")(
            TransactionTable.view(model),
          ),
        )

      case PageCase.TxDetail(_, _, _, _) =>
        div(`class` := "table-area ")(
          div(id := "oop-table-blocks", `class` := "table-list x")(
            TransactionTable.view(model),
          ),
        )

      case PageCase.AccountDetail(_, _, _, _) =>
        div(`class` := "table-area ")(
          div(id := "oop-table-blocks", `class` := "table-list x")(
            TransactionTable.view(model),
          ),
        )

      //   case PageName.BlockDetail(_) =>

      //   case PageName.TransactionDetail(_) =>
      //     TransactionDetailView.view(model)

      //   case PageName.NoPage =>
      //     NoPageView.view(model)

      //   case PageName.AccountDetail(_) =>
      //     div(`class` := "table-area")(
      //       div(id := "oop-table-blocks", `class` := "table-list x")(
      //         TransactionTable.view(model),
      //       ),
      //     )
      //   case PageName.NftDetail(_) =>
      //     div(`class` := "table-area")(
      //       div(id := "oop-table-blocks", `class` := "table-list x")(
      //         TransactionTable.view(model),
      //       ),
      //     )
      // case _ => div()

object CommonTableView:
  def view(model: Model): Html[Msg] =
    Tables.render(model)
