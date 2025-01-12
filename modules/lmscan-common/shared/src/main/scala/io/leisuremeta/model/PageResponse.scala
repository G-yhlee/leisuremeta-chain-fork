package io.leisuremeta.chain.lmscan.common.model

import sttp.tapir.generic.auto.*

final case class PageResponse[T](
    totalCount: Long,
    totalPages: Int,
    payload: Seq[T],
)
