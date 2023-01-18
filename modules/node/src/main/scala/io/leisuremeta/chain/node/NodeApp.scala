package io.leisuremeta.chain
package node

import cats.effect.{Async, IO}
import cats.effect.std.Dispatcher
import cats.effect.kernel.Resource
import cats.syntax.apply.given
import cats.syntax.functor.given
import cats.syntax.flatMap.given

import com.linecorp.armeria.server.Server
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.armeria.cats.ArmeriaCatsServerInterpreter

import api.{LeisureMetaChainApi as Api}
import api.model.{Account, Block, GroupId, PublicKeySummary, Transaction}
import api.model.account.EthAddress
import api.model.token.{TokenDefinitionId, TokenId}
import dapp.PlayNommState
import lib.crypto.{CryptoOps, KeyPair}
import lib.crypto.Hash.ops.*
import repository.{BlockRepository, GenericStateRepository, TransactionRepository}
import service.{
  LocalGossipService,
  LocalStatusService,
  NodeInitializationService,
  PeriodicActionService,
  RewardService,
  StateReadService,
  TransactionService,
}
import service.interpreter.LocalGossipServiceInterpreter
import io.leisuremeta.chain.node.service.BlockService

final case class NodeApp[F[_]
  : Async: BlockRepository: GenericStateRepository.AccountState: GenericStateRepository.GroupState: GenericStateRepository.TokenState: GenericStateRepository.RewardState: TransactionRepository: PlayNommState](
    config: NodeConfig,
):

  /** ****************************************************************************
    * Setup Endpoints
    * ****************************************************************************
    */

  import java.time.Instant
  import api.model.{Block, Signed, StateRoot}
  import lib.crypto.Hash
  import lib.datatype.{BigNat, UInt256}

  val nodeAddresses: IndexedSeq[PublicKeySummary] = config.wire.peers.map {
    peer =>
      PublicKeySummary
        .fromHex(peer.publicKeySummary)
        .getOrElse(
          throw new IllegalArgumentException(
            s"invalid pub key summary: ${peer.publicKeySummary}",
          ),
        )
  }
  val localKeyPair: KeyPair =
    val privateKey = scala.sys.env
      .get("BBGO_PRIVATE_KEY")
      .map(BigInt(_, 16))
      .orElse(config.local.`private`)
      .get
    CryptoOps.fromPrivate(privateKey)

  def getLocalGossipService(
      bestConfirmedBlock: Block,
  ): F[LocalGossipService[F]] =

    val params = GossipDomain.GossipParams(
      nodeAddresses = nodeAddresses.zipWithIndex.map { case (address, i) =>
        i -> address
      }.toMap,
      timeWindowMillis = config.wire.timeWindowMillis,
      localKeyPair = localKeyPair,
    )
    scribe.debug(s"local gossip params: $params")
    LocalGossipServiceInterpreter
      .build[F](
        bestConfirmedBlock = bestConfirmedBlock,
        params = params,
      )

  def getAccountServerEndpoint = Api.getAccountEndpoint.serverLogic {
    (a: Account) =>
      StateReadService.getAccountInfo(a).map {
        case Some(info) => Right(info)
        case None       => Left(Right(Api.NotFound(s"account not found: $a")))
      }
  }

  def getEthServerEndpoint = Api.getEthEndpoint.serverLogic {
    (ethAddress: EthAddress) =>
      StateReadService.getEthAccount(ethAddress).map {
        case Some(account) => Right(account)
        case None =>
          Left(Right(Api.NotFound(s"account not found: $ethAddress")))
      }
  }

  def getGroupServerEndpoint = Api.getGroupEndpoint.serverLogic {
    (g: GroupId) =>
      StateReadService.getGroupInfo(g).map {
        case Some(info) => Right(info)
        case None       => Left(Right(Api.NotFound(s"group not found: $g")))
      }
  }

  def getBlockListServerEndpoint = Api.getBlockListEndpoint.serverLogic {
    (fromOption, limitOption) =>
      BlockService
        .index(fromOption, limitOption)
        .leftMap { (errorMsg: String) =>
          Left(Api.ServerError(errorMsg))
        }
        .value
  }

  def getBlockServerEndpoint = Api.getBlockEndpoint.serverLogic {
    (blockHash: Block.BlockHash) =>
      val result = BlockService.get(blockHash).value

      result.map {
        case Right(Some(block)) => Right(block)
        case Right(None) =>
          Left(Right(Api.NotFound(s"block not found: $blockHash")))
        case Left(err) => Left(Left(Api.ServerError(err)))
      }
  }

  def getStatusServerEndpoint = Api.getStatusEndpoint.serverLogicSuccess { _ =>
    LocalStatusService
      .status[F](
        networkId = config.local.networkId,
        genesisTimestamp = config.genesis.timestamp,
      )
  }

  def getTokenDefServerEndpoint = Api.getTokenDefinitionEndpoint.serverLogic {
    (tokenDefinitionId: TokenDefinitionId) =>
      StateReadService.getTokenDef(tokenDefinitionId).map {
        case Some(tokenDef) => Right(tokenDef)
        case None =>
          Left(
            Right(
              Api.NotFound(s"token definition not found: $tokenDefinitionId"),
            ),
          )
      }
  }

  def getBalanceServerEndpoint = Api.getBalanceEndpoint.serverLogic {
    (account, movable) =>
      StateReadService.getBalance(account, movable).map { balanceMap =>
        Either.cond(
          balanceMap.nonEmpty,
          balanceMap,
          Right(Api.NotFound(s"balance not found: $account")),
        )
      }
  }

  def getNftBalanceServerEndpoint = Api.getNftBalanceEndpoint.serverLogic {
    (account, movable) =>
      StateReadService.getNftBalance(account, movable).map { nftBalanceMap =>
        Either.cond(
          nftBalanceMap.nonEmpty,
          nftBalanceMap,
          Right(Api.NotFound(s"nft balance not found: $account")),
        )
      }
  }

  def getTokenServerEndpoint = Api.getTokenEndpoint.serverLogic {
    (tokenId: TokenId) =>
      StateReadService.getToken(tokenId).value.map {
        case Right(Some(nftState)) => Right(nftState)
        case Right(None) =>
          Left(Right(Api.NotFound(s"token not found: $tokenId")))
        case Left(err) => Left(Left(Api.ServerError(err)))
      }
  }

  def getOwnersServerEndpoint = Api.getOwnersEndpoint.serverLogic {
    (tokenDefinitionId: TokenDefinitionId) =>
      StateReadService
        .getOwners(tokenDefinitionId)
        .leftMap { (errMsg) =>
          Left(Api.ServerError(errMsg))
        }
        .value
  }

  def getAccountActivityServerEndpoint = Api.getAccountActivityEndpoint.serverLogic{
    (account: Account) =>
      StateReadService.getAccountActivity(account).leftMap {
        case Right(msg) => Right(Api.BadRequest(msg))
        case Left(msg) => Left(Api.ServerError(msg))
      }
      .value
  }

  def getTokenActivityServerEndpoint = Api.getTokenActivityEndpoint.serverLogic{
    (tokenId: TokenId) =>
      StateReadService.getTokenActivity(tokenId).leftMap {
        case Right(msg) => Right(Api.BadRequest(msg))
        case Left(msg) => Left(Api.ServerError(msg))
      }
      .value
  }

  def postTxServerEndpoint(using LocalGossipService[F]) =
    Api.postTxEndpoint.serverLogic { (txs: Seq[Signed.Tx]) =>
      scribe.info(s"received postTx request: $txs")
      val result = TransactionService.submit[F](txs).value
      result.map {
        case Left(err) =>
          scribe.info(s"error occured in tx $txs: $err")
          Left(Right(Api.BadRequest(err)))
        case Right(txHashes) =>
          scribe.info(s"submitted txs: $txHashes")
          Right(txHashes)
      }
    }

  def getTxSetServerEndpoint = Api.getTxSetEndpoint.serverLogic {
    (block: Block.BlockHash) =>
      TransactionService
        .index(block)
        .leftMap {
          case Left(serverErrorMsg) => Left(Api.ServerError(serverErrorMsg))
          case Right(errorMessage)  => Right(Api.NotFound(errorMessage))
        }
        .value
  }

  def getTxServerEndpoint = Api.getTxEndpoint.serverLogic {
    (txHash: Signed.TxHash) =>
      scribe.info(s"received getTx request: $txHash")
      val result = TransactionService.get(txHash).value

      result.map {
        case Left(err) =>
          scribe.info(s"error occured in getting tx $txHash: $err")
        case Right(tx) =>
          scribe.info(s"got tx: $tx")
      }

      result.map {
        case Right(Some(tx)) => Right(tx)
        case Right(None) => Left(Right(Api.NotFound(s"tx not found: $txHash")))
        case Left(err)   => Left(Left(Api.ServerError(err)))
      }
  }

  def postTxHashServerEndpoint(using LocalGossipService[F]) =
    Api.postTxHashEndpoint.serverLogicPure[F] { (txs: Seq[Transaction]) =>
      scribe.info(s"received postTxHash request: $txs")
      Right(txs.map(_.toHash))
    }
/*
  def getRewardServerEndpoint = Api.getRewardEndpoint.serverLogic {
    (
        account: Account,
        timestamp: Option[Instant],
        daoAccount: Option[Account],
        rewardAmount: Option[BigNat],
    ) =>
      RewardService
        .getRewardInfoFromBestHeader(account, timestamp, daoAccount, rewardAmount)
        .value
        .map {
          case Left(err) => Left(Left(Api.ServerError(err)))
          case Right(rewardInfo) => Right(rewardInfo)
        }
  }
*/
  def leisuremetaEndpoints(using
      LocalGossipService[F],
  ): List[ServerEndpoint[Fs2Streams[F], F]] = List(
    getAccountServerEndpoint,
    getEthServerEndpoint,
    getBlockListServerEndpoint,
    getBlockServerEndpoint,
    getGroupServerEndpoint,
    getStatusServerEndpoint,
    getTxServerEndpoint,
    getTokenDefServerEndpoint,
    getBalanceServerEndpoint,
    getNftBalanceServerEndpoint,
    getTokenServerEndpoint,
    getOwnersServerEndpoint,
    getTxSetServerEndpoint,
    getAccountActivityServerEndpoint,
    getTokenActivityServerEndpoint,
//    getRewardServerEndpoint,
    postTxServerEndpoint,
    postTxHashServerEndpoint,
  )

  val localPublicKeySummary: PublicKeySummary =
    PublicKeySummary.fromPublicKeyHash(localKeyPair.publicKey.toHash)

  val localNodeIndex: Int =
    config.wire.peers.map(_.publicKeySummary).indexOf(localPublicKeySummary)

//  def periodicResource(using LocalGossipService[F]): Resource[F, F[Unit]] =
//    PeriodicActionService.periodicAction[F](
//      timeWindowMillis = config.wire.timeWindowMillis,
//      numberOfNodes = config.wire.peers.size,
//      localNodeIndex = localNodeIndex,
//    )

  def getServer(
      dispatcher: Dispatcher[F],
  ): F[Server] = for
    initializeResult <- NodeInitializationService
      .initialize[F](config.genesis.timestamp)
      .value
    bestBlock <- initializeResult match
      case Left(err)    => Async[F].raiseError(Exception(err))
      case Right(block) => Async[F].pure(block)
    given LocalGossipService[F] <- getLocalGossipService(bestBlock)
    server <- Async[F].async_[Server] { cb =>
      val tapirService = ArmeriaCatsServerInterpreter[F](dispatcher)
        .toService(leisuremetaEndpoints)
      val server = Server.builder
        .maxRequestLength(128 * 1024 * 1024)
        .requestTimeout(java.time.Duration.ofMinutes(10))
        .http(config.local.port.value)
        .service(tapirService)
        .build
      server.start.handle[Unit] {
        case (_, null)  => cb(Right(server))
        case (_, cause) => cb(Left(cause))
      }
    }
  yield server

  def resource: F[Resource[F, Server]] = Async[F].delay {
    for
//      _ <- periodicResource
      dispatcher <- Dispatcher.parallel[F]
      server <- Resource.make(getServer(dispatcher))(server =>
        Async[F]
          .fromCompletableFuture(Async[F].delay(server.closeAsync()))
          .map(_ => ()),
      )
    yield server
  }
