package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import Builder.getPage
object NavView:

  def view(model: Model): Html[Msg] =
    nav(`class` := "")(
      div(id := "title", onClick(PageMsg.PreUpdate(PageCase.Blocks())))(
        span(id := "head")(img(id := "head-logo")),
      ),
      div(
        id := "buttons",
      )(
        button(
          `class` := s"${PageCase.Blocks() == getPage(model.observers, model.observerNumber)}",
          onClick(PageMsg.PreUpdate(PageCase.Blocks())),
        )(span()(PageCase.Blocks().name)),
      ),
      // div(
      //   id := "buttons",
      // )(
      //   button(
      //     `class` := s"${PageCase.Blocks() == getPage(model.observers, model.observerNumber)}",
      //     onClick(PageMsg.PreUpdate(PageCase.Transactions())),
      //   )(span()(PageCase.Transactions().name)),
      // ),
    )
