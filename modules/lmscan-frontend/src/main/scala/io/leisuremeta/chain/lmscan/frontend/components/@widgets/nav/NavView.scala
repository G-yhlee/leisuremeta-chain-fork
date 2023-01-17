package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object NavView:
  def view(model: Model): Html[Msg] =
    nav(`class` := "")(
      div(id := "playnomm")("playNomm"),
      div(
        id := "buttons",
      )(
        button(
          `class` := s"${model.curPage.toString() == NavMsg.DashBoard.toString()}",
          onClick(NavMsg.DashBoard),
        )(NavMsg.DashBoard.toString()),
        button(
          // `class` := s"${model.curPage.toString() == NavMsg.Blocks.toString()}",
          `class` := s"${List(NavMsg.Blocks.toString(), NavMsg.BlockDetail.toString())
              .contains(model.curPage.toString())}",
          onClick(NavMsg.Blocks),
        )(NavMsg.Blocks.toString()),
        button(
          `class` := s"${model.curPage.toString() == NavMsg.Transactions.toString()}",
          onClick(NavMsg.Transactions),
        )(NavMsg.Transactions.toString()),
      ),
    )
