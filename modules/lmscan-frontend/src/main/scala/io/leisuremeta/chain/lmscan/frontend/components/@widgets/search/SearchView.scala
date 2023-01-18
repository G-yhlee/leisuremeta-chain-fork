package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object SearchView:
  def view(model: Model): Html[Msg] =
    div(`class` := "search-area")(
      div(`class` := "search-container xy-center")(
        input(
          onInput(s => InputMsg.Get(s)),
          value   := s"${model.searchValue}",
          `class` := "search-text xy-center DOM-search ",
          `placeholder` := (
            "block number, block hash, account, tx hash..",
          ),
        ),
        div(`class` := "search-icon xy-center material-symbols-outlined")(
          "search",
        ),
      ),
    )