// package io.leisuremeta.chain.lmscan.frontend

// import tyrian.Html.*
// import tyrian.*
// import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
// import io.circe.syntax.*
// import Dom.{_hidden, timeAgo, yyyy_mm_dd_time}
// import V.*
// import io.leisuremeta.chain.lmscan.common.model.AccountDetail

// import Log.*
// import io.leisuremeta.chain.lmscan.common.model.*

// object DataProcess:
//   def block(model: Model) =

//   def nft(model: Model) =

//   def blockDetail_tx(model: Model) =
//     // val data: BlockDetail = BlockDetailParser.decodeParser(model.blockDetailData.get).getOrElse(new BlockDetail)
//     // val payload = getOptionValue(data.txs, List()).asInstanceOf[List[Tx]]
//     // payload
//     val data: BlockDetail = BlockDetailParser
//       .decodeParser(model.blockDetailData.get)
//       .getOrElse(new BlockDetail)
//     getOptionValue(data.txs, List()).asInstanceOf[List[TxInfo]]

//   def acountDetail_tx(model: Model) =
//     // val data: AccountDetail = AccountDetailParser.decodeParser(model.accountDetailData.get).getOrElse(new AccountDetail)
//     // val payload = getOptionValue(data.txHistory, List()).asInstanceOf[List[Tx]]
//     // payload
//     val data: AccountDetail = AccountDetailParser
//       .decodeParser(model.accountDetailData.get)
//       .getOrElse(new AccountDetail)
//     getOptionValue(data.txHistory, List()).asInstanceOf[List[TxInfo]]

//   def dashboard_tx(model: Model) =
//     // val data: TxList = TxParser.decodeParser(model.txListData.get).getOrElse(new TxList)
//     // val payload = getOptionValue(data.payload, List()).asInstanceOf[List[Tx]]
//     // payload
//     val data: PageResponse[TxInfo] = TxParser
//       .decodeParser(model.txListData.get)
//       .getOrElse(new PageResponse(0, 0, List()))
//     data.payload.toList