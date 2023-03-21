package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import io.circe.syntax.*
import scala.scalajs.js
import Dom.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.*
import io.leisuremeta.chain.lmscan.frontend.Log.log

case class ApiPayload(page: String)

object UnderDataProcess:

  private def onResponse(pub: PubCase): Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    val parseResult: Either[ParsingFailure, Json] = parse(response.body)
    parseResult match
      case Left(parsingError) =>
        // PageMsg.GetError(s"Invalid JSON object: ${parsingError.message}", page)
        PageMsg.BackObserver
      case Right(json) => {
        PageMsg.DataUpdate(
          pub match
            case PubCase.blockPub(_, _, _) =>
              PubCase.blockPub(pub_m1 = response.body)
            case _ => PubCase.blockPub(pub_m1 = response.body),
        )

      }

  private val onError: HttpError => Msg = e =>
    // dom.document
    //   .querySelector("#loader-container")
    //   .asInstanceOf[HTMLElement]
    //   .style
    //   .display = "none"
    // ApiMsg.GetError(e.toString)
    PageMsg.BackObserver

  def fromHttpResponse(pub: PubCase): Decoder[Msg] =
    // dom.document.querySelector("#loader-container").asInstanceOf[HTMLElement].style.display = "none"
    Decoder[Msg](onResponse(pub), onError)

object OnDataProcess:
  // val host = js.Dynamic.global.process.env.BACKEND_URL
  // val port = js.Dynamic.global.process.env.BACKEND_PORT

  // !FIX
  // var base = js.Dynamic.global.process.env.BASE_API_URL_DEV
  var base = js.Dynamic.global.process.env.BASE_API_URL

  def getData(
      pub: PubCase,
      // payload: ApiPayload = ApiPayload("1"),
  ): Cmd[IO, Msg] =
    // dom.document
    //   .querySelector("#loader-container")
    //   .asInstanceOf[HTMLElement]
    //   .style
    //   .display = "block"

    val url = pub match
      case PubCase.blockPub(_, _, _) =>
        s"$base/block/list?pageNo=${(0).toString()}&sizePerRequest=10"

      case _ =>
        s"$base/block/list?pageNo=${(0).toString()}&sizePerRequest=10"

    Http.send(
      Request.get(url).withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(pub),
    )
