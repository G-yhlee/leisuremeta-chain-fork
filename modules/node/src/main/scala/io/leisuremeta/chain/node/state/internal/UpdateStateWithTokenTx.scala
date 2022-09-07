package io.leisuremeta.chain
package node
package state
package internal

import java.time.Instant

import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.either.given
import cats.syntax.eq.given
import cats.syntax.foldable.given
import cats.syntax.traverse.given

import scodec.bits.BitVector

import GossipDomain.MerkleState
import UpdateState.*
import api.model.{
  Account,
  AccountSignature,
  GroupData,
  GroupId,
  PublicKeySummary,
  Signed,
  Transaction,
  TransactionWithResult,
}
import api.model.token.*
import api.model.TransactionWithResult.ops.*
import lib.merkle.{MerkleTrie, MerkleTrieState}
import lib.codec.byte.{ByteDecoder, DecodeResult}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.{BigNat, Utf8}
import repository.{StateRepository, TransactionRepository}
import repository.StateRepository.given
import io.leisuremeta.chain.node.service.StateReadService

trait UpdateStateWithTokenTx:

  given updateStateWithTokenTx[F[_]
    : Concurrent: StateRepository.TokenState: StateRepository.GroupState: StateRepository.AccountState: TransactionRepository]
      : UpdateState[F, Transaction.TokenTx] =
    (ms: MerkleState, sig: AccountSignature, tx: Transaction.TokenTx) =>
      tx match
        case dt: Transaction.TokenTx.DefineToken =>
          for
            tokenDefinitionOption <- MerkleTrie
              .get[F, TokenDefinitionId, TokenDefinition](
                dt.definitionId.toBytes.bits,
              )
              .runA(ms.token.tokenDefinitionState)
            _ <- EitherT.cond(
              tokenDefinitionOption.isEmpty,
              (),
              s"Token definition ${dt.definitionId} already exists",
            )
            tokenDefinition = TokenDefinition(
              id = dt.definitionId,
              name = dt.name,
              symbol = dt.symbol,
              adminGroup = dt.minterGroup,
              totalAmount = BigNat.Zero,
              nftInfo = dt.nftInfo,
            )
            tokenDefinitionState <- MerkleTrie
              .put[F, TokenDefinitionId, TokenDefinition](
                dt.definitionId.toBytes.bits,
                tokenDefinition,
              )
              .runS(ms.token.tokenDefinitionState)
          yield
            scribe.info(
              s"===> new Token Definition state: $tokenDefinitionState",
            )
            (
              ms.copy(token =
                ms.token.copy(tokenDefinitionState = tokenDefinitionState),
              ),
              TransactionWithResult(Signed(sig, tx), None),
            )
        case mf: Transaction.TokenTx.MintFungibleToken =>
          for
            tokenDefinitionOption <- MerkleTrie
              .get[F, TokenDefinitionId, TokenDefinition](
                mf.definitionId.toBytes.bits,
              )
              .runA(ms.token.tokenDefinitionState)
            tokenDefinition <- EitherT.fromOption[F](
              tokenDefinitionOption,
              s"Token definition ${mf.definitionId} does not exist",
            )
            adminGroupId <- EitherT.fromOption[F](
              tokenDefinition.adminGroup,
              s"Admin group does not exist in token $tokenDefinition",
            )
            groupAccountOption <- MerkleTrie
              .get[F, (GroupId, Account), Unit](
                (adminGroupId, sig.account).toBytes.bits,
              )
              .runA(ms.group.groupAccountState)
            _ <- EitherT.cond(
              groupAccountOption.nonEmpty,
              (),
              s"Account ${sig.account} is not a member of admin group $adminGroupId",
            )
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(mf, sig.sig),
            )
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.cond(
              accountPubKeyOption.nonEmpty,
              (),
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            txWithResult = TransactionWithResult(Signed(sig, tx), None)
            items = mf.outputs.map { case (account, _) =>
              (account, mf.definitionId, txWithResult.toHash)
            }
            fungibleBalanceState <- items.toList.foldLeftM(
              ms.token.fungibleBalanceState,
            ) { case (state, item) =>
              MerkleTrie
                .put[
                  F,
                  (
                      Account,
                      TokenDefinitionId,
                      Hash.Value[TransactionWithResult],
                  ),
                  Unit,
                ](item.toBytes.bits, ())
                .runS(state)
            }
            totalAmount = mf.outputs.map(_._2).foldLeft(BigNat.Zero)(BigNat.add)
            tokenDefinition1 = tokenDefinition.copy(
              totalAmount = BigNat.add(tokenDefinition.totalAmount, totalAmount),
            )
            tokenDefinitionState1 <- {
              for
                _ <- MerkleTrie.remove[F, TokenDefinitionId, TokenDefinition](
                  mf.definitionId.toBytes.bits,
                )
                _ <- MerkleTrie.put[F, TokenDefinitionId, TokenDefinition](
                  mf.definitionId.toBytes.bits,
                  tokenDefinition1,
                )
              yield ()
            }.runS(ms.token.tokenDefinitionState)
          yield (
            ms.copy(token =
              ms.token.copy(
                fungibleBalanceState = fungibleBalanceState,
                tokenDefinitionState = tokenDefinitionState1,
              ),
            ),
            txWithResult,
          )
        case mn: Transaction.TokenTx.MintNFT =>
          for
            tokenDefinitionOption <- MerkleTrie
              .get[F, TokenDefinitionId, TokenDefinition](
                mn.tokenDefinitionId.toBytes.bits,
              )
              .runA(ms.token.tokenDefinitionState)
            tokenDefinition <- EitherT.fromOption[F](
              tokenDefinitionOption,
              s"Token definition ${mn.tokenDefinitionId} does not exist",
            )
            adminGroupId <- EitherT.fromOption[F](
              tokenDefinition.adminGroup,
              s"Admin group does not exist in token $tokenDefinition",
            )
            groupAccountOption <- MerkleTrie
              .get[F, (GroupId, Account), Unit](
                (adminGroupId, sig.account).toBytes.bits,
              )
              .runA(ms.group.groupAccountState)
            _ <- EitherT.cond(
              groupAccountOption.nonEmpty,
              (),
              s"Account ${sig.account} is not a member of admin group $adminGroupId",
            )
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(mn, sig.sig),
            )
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.cond(
              accountPubKeyOption.nonEmpty,
              (),
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            txWithResult = TransactionWithResult(Signed(sig, mn), None)
            balanceItem  = (mn.output, mn.tokenId, txWithResult.toHash)
            nftBalanceState <- MerkleTrie
              .put[
                F,
                (Account, TokenId, Hash.Value[TransactionWithResult]),
                Unit,
              ](balanceItem.toBytes.bits, ())
              .runS(ms.token.nftBalanceState)
            nftStateItem = NftState(mn.tokenId, mn.tokenDefinitionId, mn.output)
            nftState <- MerkleTrie
              .put[F, TokenId, NftState](mn.tokenId.toBytes.bits, nftStateItem)
              .runS(ms.token.nftState)
            rarityItem = (mn.tokenDefinitionId, mn.rarity, mn.tokenId)
            rarityState <- MerkleTrie
              .put[F, (TokenDefinitionId, Rarity, TokenId), Unit](
                rarityItem.toBytes.bits,
                (),
              )
              .runS(ms.token.rarityState)
            tokenState = ms.token.copy(
              nftBalanceState = nftBalanceState,
              nftState = nftState,
              rarityState = rarityState,
            )
          yield (ms.copy(token = tokenState), txWithResult)
        case tf: Transaction.TokenTx.TransferFungibleToken =>
          val txWithResult = TransactionWithResult(Signed(sig, tx), None)

          type FungibleBalance =
            (Account, TokenDefinitionId, Hash.Value[TransactionWithResult])

          val transferFungibleTokenProgram: StateT[
            EitherT[F, String, *],
            MerkleTrieState[FungibleBalance, Unit],
            Unit,
          ] = for
            _ <- tf.inputs.toList.traverse { (txHash) =>
              MerkleTrie.remove[F, FungibleBalance, Unit](
                (sig.account, tf.tokenDefinitionId, txHash).toBytes.bits,
              )
            }
            _ <- tf.outputs.toList.traverse { (account, amount) =>
              MerkleTrie.put[F, FungibleBalance, Unit](
                (
                  account,
                  tf.tokenDefinitionId,
                  txWithResult.toHash,
                ).toBytes.bits,
                (),
              )
            }
          yield ()

          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(tf, sig.sig),
            )
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            fungibleBalanceState <- transferFungibleTokenProgram.runS(
              ms.token.fungibleBalanceState,
            )
          yield (
            ms.copy(token =
              ms.token.copy(fungibleBalanceState = fungibleBalanceState),
            ),
            txWithResult,
          )
        case tn: Transaction.TokenTx.TransferNFT =>
          val txWithResult = TransactionWithResult(Signed(sig, tx), None)
          type NftBalance = (Account, TokenId, Hash.Value[TransactionWithResult])

          val transferNftProgram: StateT[
            EitherT[F, String, *],
            MerkleTrieState[NftBalance, Unit],
            Unit,
          ] = for
            _ <- MerkleTrie.remove[F, NftBalance, Unit]{
              (sig.account, tn.tokenId, tn.input).toBytes.bits
            }
            _ <- MerkleTrie.put[F, NftBalance, Unit](
                (
                  tn.output,
                  tn.tokenId,
                  txWithResult.toHash,
                ).toBytes.bits,
                (),
            )
          yield ()

          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(tn, sig.sig),
            )
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            nftBalanceState <- transferNftProgram.runS(
              ms.token.nftBalanceState,
            )
          yield (
            ms.copy(token =
              ms.token.copy(nftBalanceState = nftBalanceState),
            ),
            txWithResult,
          )
        case bf: Transaction.TokenTx.BurnFungibleToken => ???
        case bn: Transaction.TokenTx.BurnNFT => ???
        case ef: Transaction.TokenTx.EntrustFungibleToken =>
          type FungibleBalance =
            (Account, TokenDefinitionId, Hash.Value[TransactionWithResult])

          type EntrustFungibleBalance =
            (Account, Account, TokenDefinitionId, Hash.Value[TransactionWithResult])
          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(ef, sig.sig),
            )
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            inputAmounts <- ef.inputs.toList.traverse { (txHash: Signed.TxHash) =>
              TransactionRepository[F].get(txHash.toResultHashValue).leftMap(_.msg).flatMap{
                case Some(txWithResult) =>
                  txWithResult.signedTx.value match
                    case fb: Transaction.FungibleBalance => fb match
                      case mf: Transaction.TokenTx.MintFungibleToken =>
                        EitherT.pure(mf.outputs.get(sig.account).getOrElse(BigNat.Zero))
                      case bf: Transaction.TokenTx.BurnFungibleToken =>
                        txWithResult.result match
                          case Some(Transaction.TokenTx.BurnFungibleTokenResult(outputAmount)) =>
                            EitherT.pure(outputAmount)
                          case other => EitherT.leftT[F, BigNat](s"burn fungible token result of $txHash has wrong result: $other")
                      case tf: Transaction.TokenTx.TransferFungibleToken =>
                        EitherT.pure(tf.outputs.get(sig.account).getOrElse(BigNat.Zero))

                      case ef: Transaction.TokenTx.EntrustFungibleToken =>
                        EitherT.pure(txWithResult.result.fold(BigNat.Zero){
                          case Transaction.TokenTx.EntrustFungibleTokenResult(remainder) => remainder
                          case _ => BigNat.Zero
                        })
                      case df: Transaction.TokenTx.DisposeEntrustedFungibleToken =>
                        EitherT.pure(df.outputs.get(sig.account).getOrElse(BigNat.Zero))
                    case _ => EitherT.leftT[F, BigNat](s"input tx $txHash is not a fungible balance")
                case None =>
                  EitherT.leftT[F, BigNat](s"input tx $txHash does not exist")
              }
            }
            remainder <- EitherT.fromEither[F]{
              val inputSum = inputAmounts.map(_.toBigInt).sum 
              BigNat
                .fromBigInt(inputSum- ef.amount.toBigInt)
                .leftMap(_ => s"input sum $inputSum is less than output amount ${ef.amount}")
            }
            result = Transaction.TokenTx.EntrustFungibleTokenResult(remainder)
            txWithResult = TransactionWithResult(Signed(sig, tx), Some(result))
            fungibleBalanceState0 <- ef.inputs.toList.traverse { (txHash) =>
              MerkleTrie.remove[F, FungibleBalance, Unit](
                (sig.account, ef.definitionId, txHash).toBytes.bits,
              )
            }.runS(ms.token.fungibleBalanceState)
            fungibleBalanceState <- MerkleTrie.put[F, FungibleBalance, Unit](
              (sig.account, ef.definitionId, txWithResult.toHash).toBytes.bits,
              (),
            ).runS(fungibleBalanceState0)
            entrustFungibleBalanceState <- MerkleTrie.put[F, EntrustFungibleBalance, Unit](
              (
                sig.account,
                ef.to,
                ef.definitionId,
                txWithResult.toHash,
              ).toBytes.bits,
              (),
            ).runS(ms.token.entrustFungibleBalanceState)
          yield (
            ms.copy(token =
              ms.token.copy(
                fungibleBalanceState = fungibleBalanceState,
                entrustFungibleBalanceState = entrustFungibleBalanceState,
              ),
            ),
            txWithResult,
          )
        case en: Transaction.TokenTx.EntrustNFT => 
          val txWithResult = TransactionWithResult(Signed(sig, tx), None)
          type NftBalance = (Account, TokenId, Hash.Value[TransactionWithResult])
          type EntrustNftBalance = (Account, Account, TokenId, Hash.Value[TransactionWithResult])

          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(en, sig.sig),
            )
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            nftBalanceState <- MerkleTrie.remove[F, NftBalance, Unit]{
              (sig.account, en.tokenId, en.input).toBytes.bits
            }.runS(ms.token.nftBalanceState)
            entrustNftBalanceState <- MerkleTrie.put[F, EntrustNftBalance, Unit](
                (
                  sig.account,
                  en.to,
                  en.tokenId,
                  txWithResult.toHash,
                ).toBytes.bits,
                (),
            ).runS(ms.token.entrustNftBalanceState)
          yield (
            ms.copy(token =
              ms.token.copy(
                nftBalanceState = nftBalanceState,
                entrustNftBalanceState = entrustNftBalanceState,
              ),
            ),
            txWithResult,
          )
        case de: Transaction.TokenTx.DisposeEntrustedFungibleToken =>

          val txWithResult = TransactionWithResult(Signed(sig, tx), None)

          type FungibleBalance =
            (Account, TokenDefinitionId, Hash.Value[TransactionWithResult])

          type EntrustFungibleBalance =
            (Account, Account, TokenDefinitionId, Hash.Value[TransactionWithResult])
          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(de, sig.sig),
            )
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            inputs <- de.inputs.toList.traverse{ (txHash) =>
              TransactionRepository[F].get(txHash.toResultHashValue).leftMap(_.msg).flatMap{
                case None => EitherT.leftT(s"Entrust Input $txHash does not exist.")
                case Some(txWithResult) => txWithResult.signedTx.value match
                  case ef: Transaction.TokenTx.EntrustFungibleToken =>
                    EitherT.pure(((txWithResult.signedTx.sig.account, txHash), ef.amount.toBigInt))
                  case otherTx =>
                    EitherT.leftT(s"input tx $txHash is not EntrustFunbleToken: $otherTx")
              }
            }
            inputBalance = inputs.unzip._2.sum
            outputSum = de.outputs.values.map(_.toBigInt).sum
            _ <- EitherT.cond(
              inputBalance >= outputSum,
              (),
              s"Input balance $inputBalance is less than output sum $outputSum",
            )
            entrustFungibleBalanceState <- inputs.unzip._1.traverse { case (account, txHash) =>
              MerkleTrie.remove[F, EntrustFungibleBalance, Unit](
                (account, sig.account, de.definitionId, txHash).toBytes.bits,
              )
            }.runS(ms.token.entrustFungibleBalanceState)
            fungibleBalanceState <- de.outputs.toList.traverse{ case (account, amount) =>
              MerkleTrie.put[F, FungibleBalance, Unit](
                (account, de.definitionId, txWithResult.toHash).toBytes.bits,
                (),
              )
            }.runS(ms.token.fungibleBalanceState)
          yield (
            ms.copy(token =
              ms.token.copy(
                fungibleBalanceState = fungibleBalanceState,
                entrustFungibleBalanceState = entrustFungibleBalanceState,
              ),
            ),
            txWithResult,
          )
        case dn: Transaction.TokenTx.DisposeEntrustedNFT =>
          type NftBalance = (Account, TokenId, Hash.Value[TransactionWithResult])
          type EntrustNftBalance = (Account, Account, TokenId, Hash.Value[TransactionWithResult])
          
          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(dn, sig.sig),
            )
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            inputTxOption <- TransactionRepository[F].get(dn.input.toResultHashValue).leftMap(_.msg)
            inputTx <- inputTxOption match
              case None => EitherT.leftT(s"Input tx ${dn.input} does not exist")
              case Some(txWithResult) => txWithResult.signedTx.value match
                case eb: Transaction.TokenTx.EntrustNFT =>
                  if eb.to === sig.account then EitherT.pure((txWithResult.signedTx.sig.account, eb))
                  else EitherT.leftT(s"Input $eb is not entrusted to ${sig.account}")
                case other =>
                  EitherT.leftT(s"input tx $other is not a EntrustNft tx")
            inputAccount = inputTx._1
            outputAccount = dn.output.getOrElse(inputAccount)
            entrustNftBalanceState <- MerkleTrie.remove[F, EntrustNftBalance, Unit]{
              (inputAccount, sig.account, dn.tokenId, dn.input).toBytes.bits
            }.runS(ms.token.entrustNftBalanceState)
            txWithResult = TransactionWithResult(Signed(sig, tx), None)
            nftBalanceState <- MerkleTrie.put[F, NftBalance, Unit](
                (
                  outputAccount,
                  dn.tokenId,
                  txWithResult.toHash,
                ).toBytes.bits,
                (),
            ).runS(ms.token.nftBalanceState)
          yield (
            ms.copy(token =
              ms.token.copy(
                nftBalanceState = nftBalanceState,
                entrustNftBalanceState = entrustNftBalanceState,
              ),
            ),
            txWithResult,
          )