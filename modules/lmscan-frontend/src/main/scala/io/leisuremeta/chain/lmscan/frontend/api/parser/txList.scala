package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.common.model.TxInfo

// case class TxList(
//     totalCount: Option[Int] = None,
//     totalPages: Option[Int] = None,
//     payload: Option[List[Tx]] = None,
// )
// case class Tx(
//     hash: Option[String] = None,
//     blockNumber: Option[Int] = None,
//     createdAt: Option[Int] = None,
//     txType: Option[String] = None,
//     tokenType: Option[String] = None,
//     signer: Option[String] = None,
//     inOut: Option[String] = None,
//     value: Option[String] = None,
// )

object TxParser:
    given txlistDecoder: Decoder[PageResponse[TxInfo]] = deriveDecoder[PageResponse[TxInfo]]
    given txDecoder: Decoder[TxInfo]         = deriveDecoder[TxInfo]
    def decodeParser(body: String)       = decode[PageResponse[TxInfo]](body)
