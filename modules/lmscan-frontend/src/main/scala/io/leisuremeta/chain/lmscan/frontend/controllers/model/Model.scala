package io.leisuremeta.chain.lmscan.frontend

import io.circe.Json

// import cats.effect.IO
final case class Model(
    // STATE MODEL

    // nav move
    prevPage: PageName,
    curPage: PageName,

    // input search string
    searchValue: String,

    // transaction detail toggle
    toggle: Boolean,
    toggleTxDetailInput: Boolean,

    // page move
    tx_TotalPage: Int,
    block_TotalPage: Int,

    // page_Search: String,
    block_list_Search: String,
    tx_list_Search: String,

    // api data
    apiData: Option[String] = Some(""),
    txListData: Option[String] = Some(""),
    blockListData: Option[String] = Some(""),
    txDetailData: Option[String] = Some(""),
    blockDetailData: Option[String] = Some(""),
    nftDetailData: Option[String] = Some(""),
    accountDetailData: Option[String] = Some(""),

    // store
    searchValueStore: String = "",
    pageNameStore: PageName = PageName.DashBoard,
    urlStore: String = "",
    curDataStore: Option[String] = Some(""),

    // block
    latestBlockNumber: String = "1",

    // tx-list :: account-detail
    tx_list_data_of_account_detail: Option[String] = Some(""),
    tx_TotalPage_of_account_detail: Int = 1,
    tx_list_Search_of_account_detail: String = "1",

    // tx-list :: block-detail
    tx_list_data_of_block_detail: Option[String] = Some(""),
    tx_TotalPage_of_block_detail: Int = 1,
    tx_list_Search_of_block_detail: String = "1",

    // tx-list :: nft-detail
    tx_list_data_of_nft_detail: Option[String] = Some(""),
    tx_TotalPage_of_nft_detail: Int = 1,
    tx_list_Search_of_nft_detail: String = "1",
)
