package io.leisuremeta.chain.lmscan.agent.repository

import io.leisuremeta.chain.lmscan.agent.repository.CommonQuery
import io.leisuremeta.chain.lmscan.agent.entity.AccountEntity
import io.getquill.*
import cats.effect.kernel.Async
import cats.data.EitherT
import CommonQuery.*

object AccountRepository:
  import ctx.{*, given}
  inline given SchemaMeta[AccountEntity] = schemaMeta[AccountEntity]("account")


  def totalCount[F[_]: Async]: EitherT[F, String, Long] =
    inline def totalQuery = quote {
      query[AccountEntity]
    }
    countQuery(totalQuery)
