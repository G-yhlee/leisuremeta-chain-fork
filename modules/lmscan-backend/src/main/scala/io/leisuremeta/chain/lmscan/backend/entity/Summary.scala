package io.leisuremeta.chain.lmscan.backend.entity

final case class Summary(
    id: Long,
    lmPrice: Double,
    blockNumber: Long,
    totalTxSize: Double,
    totalAccounts: Long,
    createdAt: Long,
)
