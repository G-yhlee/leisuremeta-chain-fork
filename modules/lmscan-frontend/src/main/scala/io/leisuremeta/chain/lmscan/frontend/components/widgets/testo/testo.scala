package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
import Dom.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import io.leisuremeta.chain.lmscan.common.model.BlockInfo
import org.scalajs.dom.window

object TestoView:
  def view(model: Model): Html[Msg] =
    div(`class` := s"testo")(
      h1("testo page"),
    )
