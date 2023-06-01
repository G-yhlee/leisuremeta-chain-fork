package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import scala.compiletime.ops.any
import V.*
import java.math.RoundingMode
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.common.model.AccountDetail
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import io.leisuremeta.chain.lmscan.frontend.Log.log2

object AccountDetailTable:
  val view = (model: Model) =>
    val apiData: SummaryModel = get_PageResponseViewCase(model).board

    val data: AccountDetail = get_PageResponseViewCase(model).accountDetail
    genView(model, data, apiData)

  val genView = (model: Model, data: AccountDetail, apiData: SummaryModel) =>
    val lmPrice =
      model.lmprice
    val balance = getOptionValue[BigDecimal](data.balance, 0)
      .asInstanceOf[BigDecimal] / Math.pow(10, 18).toDouble
    val value = lmPrice * balance

    val formatter = java.text.NumberFormat.getNumberInstance()
    formatter.setRoundingMode(RoundingMode.FLOOR)

    formatter.setMaximumFractionDigits(4)
    val formattedBalance = formatter.format(balance)

    formatter.setMaximumFractionDigits(4)
    val formattedValue = formatter.format(value)

    div(`class` := "y-start gap-10px w-[100%] ")(
      div(`class` := "x")(
        div(
          `class` := "type-TableDetail table-container position-relative",
        )(
          div(`class` := "m-10px w-[100%] ")(
            div()(
              div(`class` := "table w-[100%] ")(
                div(`class` := "row")(
                  gen.cell(
                    Cell.Head("Account", "cell type-detail-head"),
                    Cell.PlainStr(data.address, "cell type-detail-body"),
                  ),
                ),
                div(`class` := "row")(
                  gen.cell(
                    Cell.Head("Balance", "cell type-detail-head"),
                    Cell.Any(
                      formattedBalance.toString() + " LM",
                      "cell type-detail-body",
                    ),
                  ),
                ),
                div(`class` := "row")(
                  gen.cell(
                    Cell.Head("Value", "cell type-detail-head"),
                    Cell.Any(
                      "$ " + formattedValue.toString(),
                      "cell type-detail-body",
                    ),
                  ),
                ),
              ),
            ),
            data != new AccountDetail match
              case false => LoaderView.view(model)
              case _     => div(`class` := "")(),
          ),
        ),
      ),
    )
