package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import io.leisuremeta.chain.lmscan.frontend.Builder.getPage

object Title:
  def block = (model: Model) =>
    div(
      //   `class` := s"${State.curPage(model, PageName.DashBoard, "_table-title")} table-title ",
    )(
      div(
        `class` := s"type-1",
      )(span()("Latest Blocks")),
      div(
        `class` := s"type-2",
      )(
        // {
        //   getPage(model.observers, model.observerNumber) match
        //     case PageCase.DashBoard =>
        //       span(
        //         onClick(PageMsg.PreUpdate(PageName.Blocks(1))),
        //       )("More")
        //     case _ => div()
        // },
      ),
    )
//   def tx = (model: Model) =>
//     div(
//       `class` := s"${State.curPage(model, PageName.DashBoard, "_table-title")} table-title ",
//     )(
//       div(
//         `class` := s"type-1",
//       )(span()("Latest transactions")),
//       div(
//         `class` := s"type-2",
//       )(
//         span(
//           onClick(PageMsg.PreUpdate(PageName.Transactions(1))),
//         )("More"),
//       ),
//     )

//   def nft = (model: Model) =>
//     div(
//       `class` := s"table-title ",
//     )(
//       div(
//         `class` := s"type-1",
//       )(span()("Item Activity")),
//     )
