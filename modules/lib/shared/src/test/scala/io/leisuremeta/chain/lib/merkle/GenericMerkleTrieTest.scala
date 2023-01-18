package io.leisuremeta.chain.lib
package merkle

import scala.concurrent.ExecutionContext.Implicits.global

import hedgehog.munit.HedgehogSuite
import hedgehog.*
import hedgehog.state.*
import java.io.File
import java.io.FileWriter

import scala.collection.immutable.SortedMap

import cats.{~>, Monad, Id}
import cats.arrow.FunctionK
import cats.catsInstancesForId
import cats.data.{EitherT, Kleisli, StateT}
import cats.implicits.{*, given}
import cats.syntax.all.{*, given}

import fs2.Stream
import scodec.bits.{BitVector, ByteVector}
import scodec.bits.hex

import codec.byte.{ByteDecoder, ByteEncoder}
import datatype.BigNat
import GenericMerkleTrie.NodeStore
import GenericMerkleTrieNode.{MerkleHash, MerkleRoot}

type K = ByteVector
type V = ByteVector

given ByteEncoder[ByteVector] = (bytes: ByteVector) =>
  import ByteEncoder.ops.*
  BigNat.unsafeFromBigInt(bytes.size).toBytes ++ bytes

given ByteDecoder[ByteVector] =
  ByteDecoder[BigNat].flatMap { size =>
    ByteDecoder.fromFixedSizeBytes(size.toBigInt.toLong)(identity)
  }

case class State(
    current: SortedMap[K, V],
    hashLog: Map[SortedMap[K, V], Option[MerkleRoot[K, V]]],
)
object State:
  def empty: State = State(SortedMap.empty, Map.empty)

case class Get(key: BitVector)
case class Put(key: BitVector, value: ByteVector)
case class Remove(key: BitVector)
case class From(key: BitVector)

given emptyNodeStore[K, V]: NodeStore[Id, K, V] =
  Kleisli { (_: MerkleHash[K, V]) => EitherT.rightT[Id, String](None) }

var merkleTrieState: GenericMerkleTrieState[K, V] =
  GenericMerkleTrieState.empty[K, V]

val genByteVector = Gen.bytes(Range.linear(0, 64)).map(ByteVector.view)
def commandGet: CommandIO[State] = new Command[State, Get, Option[V]]:

  override def gen(s: State): Option[Gen[Get]] = Some(
    (s.current.keys.toList match
      case Nil => genByteVector
      case h :: t =>
        Gen.frequency1(
          80 -> Gen.element(h, t),
          20 -> genByteVector,
        )
    ).map(bytes => Get(bytes.bits)),
  )
  override def execute(env: Environment, i: Get): Either[String, Option[V]] =
    val program = GenericMerkleTrie.get[Id, K, V](i.key)
    program.runA(merkleTrieState).value

  override def update(s: State, i: Get, o: Var[Option[V]]): State = s

  override def ensure(
      env: Environment,
      s0: State,
      s: State,
      i: Get,
      o: Option[V],
  ): Result = s.current.get(i.key.bytes) ==== o

def commandPut: CommandIO[State] = new Command[State, Put, Unit]:

  override def gen(s: State): Option[Gen[Put]] =
    Some(for
      key   <- genByteVector
      value <- genByteVector
    yield Put(key.toBitVector, value))

  override def execute(env: Environment, i: Put): Either[String, Unit] =
//    println(s"===> execute: $i")

    val program = GenericMerkleTrie.put[Id, K, V](i.key, i.value)
    program.runS(merkleTrieState).value.map {
      (newState: GenericMerkleTrieState[K, V]) =>
        merkleTrieState = newState
    }

  override def update(s: State, i: Put, o: Var[Unit]): State =

//    println(s"===> update: ${s.current}")
    val current1  = s.current + ((i.key.bytes -> i.value))
    val stateRoot = merkleTrieState.root
    val hashLog1  = s.hashLog + ((current1    -> stateRoot))
    State(current1, hashLog1)

  override def ensure(
      env: Environment,
      s0: State,
      s: State,
      i: Put,
      o: Unit,
  ): Result = Result.all(
    List(
//      s0.hashLog.get(s.current).fold(Result.success) {
//        (rootOption: Option[MerkleRoot[K, V]]) =>
//          if s.hashLog.get(s.current) != Some(rootOption) then
//            println(s"===> current: ${s.current}")
//          s.hashLog.get(s.current) ==== Some(rootOption)
//      },
      merkleTrieState.root.fold(Result.success) { (root: MerkleRoot[K, V]) =>
        val result = merkleTrieState.diff.get(root).nonEmpty
        if result == false then
          println(s"====> failed: $i with state ${s0.current}")
        Result.assert(result)
      },
      s.current.get(i.key.bytes) ==== Some(i.value),
    ),
  )

def commandRemove: CommandIO[State] = new Command[State, Remove, Boolean]:

  override def gen(s: State): Option[Gen[Remove]] = Some(
    (s.current.keys.toList match
      case Nil => genByteVector
      case h :: t =>
        Gen.frequency1(
          80 -> Gen.element(h, t),
          20 -> genByteVector,
        )
    ).map(bytes => Remove(bytes.bits)),
  )
  override def execute(env: Environment, i: Remove): Either[String, Boolean] =
    val program = GenericMerkleTrie.remove[Id, K, V](i.key)
    program.run(merkleTrieState).value.map { case (state1, result) =>
      merkleTrieState = state1
      result
    }

  override def update(s: State, i: Remove, o: Var[Boolean]): State =
    val current1  = s.current - i.key.bytes
    val stateRoot = merkleTrieState.root
    val hashLog1  = s.hashLog + ((current1 -> stateRoot))
    State(current1, s.hashLog)

  override def ensure(
      env: Environment,
      s0: State,
      s: State,
      i: Remove,
      o: Boolean,
  ): Result = Result.all(
    List(
      s0.current.contains(i.key.bytes) ==== o,
      s.current.get(i.key.bytes) ==== None,
    ),
  )

type S = Stream[EitherT[Id, String, *], (BitVector, V)]
def commandFrom: CommandIO[State] = new Command[State, From, S]:

  override def gen(s: State): Option[Gen[From]] = Some(
    (s.current.keys.toList match
      case Nil => genByteVector
      case h :: t =>
        Gen.frequency1(
          80 -> Gen.element(h, t),
          20 -> genByteVector,
        )
    ).map(bytes => From(bytes.bits)),
  )
  override def execute(env: Environment, i: From): Either[String, S] =
    val program = GenericMerkleTrie.from[Id, K, V](i.key)
    program.runA(merkleTrieState).value

  override def update(s: State, i: From, o: Var[S]): State = s

  override def ensure(
      env: Environment,
      s0: State,
      s: State,
      i: From,
      o: S,
  ): Result =
    import fs2.given
    import cats.implicits.*
    import cats.effect.unsafe.implicits.global

    val toId = new FunctionK[EitherT[Id, String, *], Id]:
      override def apply[A](fa: EitherT[Id, String, A]): Id[A] =
        fa.value.toOption.get

    val expected =
      s.current.iteratorFrom(i.key.bytes).take(10).toList.map { (k, v) =>
        (k.bits, v)
      }

    expected ==== o
      .take(10)
      .translate[EitherT[Id, String, *], Id](toId)
      .compile
      .toList

class GenericMerkleTrieTest extends HedgehogSuite:
  test("put same key value twice expect not to change state") {
    withMunitAssertions { assertions =>
      val initialState = GenericMerkleTrieState.empty[K, V]
      val program =
        GenericMerkleTrie.put[Id, K, V](ByteVector.empty.bits, ByteVector.empty)

      val resultEitherT = for
        state1 <- program.runS(initialState)
        state2 <- program.runS(state1)
      yield assertions.assertEquals(state1, state2)

      resultEitherT.value
    }
  }

  test("put 10 -> put empty with empty -> put 10") {
    withMunitAssertions { assertions =>
      val initialState = GenericMerkleTrieState.empty[K, V]
      val put10 =
        GenericMerkleTrie.put[Id, K, V](hex"10".bits, ByteVector.empty)
      val putEmptyWithEmpty =
        GenericMerkleTrie.put[Id, K, V](ByteVector.empty.bits, ByteVector.empty)

//    val forPrint = for
//      state1 <- put10.runS(initialState)
//      state2 <- putEmptyWithEmpty.runS(state1)
//      state3 <- put10.runS(state2)
//    yield
//      Seq(state1, state2, state3).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      val initialProgram = for
        _ <- put10
        _ <- putEmptyWithEmpty
      yield ()

      val resultEitherT = for
        state1 <- initialProgram.runS(initialState)
        state2 <- put10.runS(state1)
      yield assertions.assertEquals(state1, state2)

      resultEitherT.value
    }
  }

  test(
    "put (10, empty) -> put (empty, empty) -> put (10, 00) -> put (10, empty)",
  ) {
    withMunitAssertions { assertions =>

      val initialState = GenericMerkleTrieState.empty[K, V]
      val put10withEmpty =
        GenericMerkleTrie.put[Id, K, V](hex"10".bits, ByteVector.empty)
      val putEmptyWithEmpty =
        GenericMerkleTrie.put[Id, K, V](ByteVector.empty.bits, ByteVector.empty)
      val put10with10 =
        GenericMerkleTrie.put[Id, K, V](hex"10".bits, hex"10")

//    val forPrint = for
//      state1 <- put10withEmpty.runS(initialState)
//      _ <- EitherT.pure[Id, String](println(s"===> state1: ${state1}"))
//      state2 <- putEmptyWithEmpty.runS(state1)
//      _ <- EitherT.pure[Id, String](println(s"===> state2: ${state2}"))
//      state3 <- put10with10.runS(state2)
//      _ <- EitherT.pure[Id, String](println(s"===> state3: ${state3}"))
//      state4 <- put10withEmpty.runS(state3)
//      _ <- EitherT.pure[Id, String](println(s"===> state4: ${state4}"))
//    yield
//      Seq(state1, state2, state3, state4).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      val program = for
        _ <- put10withEmpty
        _ <- putEmptyWithEmpty
        _ <- put10with10
        _ <- put10withEmpty
      yield ()

      program.runS(initialState).value match
        case Right(state) =>
          assertions.assert(state.diff.get(state.root.get).nonEmpty)
        case Left(error) =>
          assertions.fail(error)
    }
  }

  test("put (empty, empty) -> put (00, 00) -> get (empty)") {
    withMunitAssertions { assertions =>
      val initialState = GenericMerkleTrieState.empty[K, V]
      val putEmptyWithEmpty =
        GenericMerkleTrie.put[Id, K, V](ByteVector.empty.bits, ByteVector.empty)
      val put00_00 =
        GenericMerkleTrie.put[Id, K, V](hex"00".bits, hex"00")
      val getEmpty =
        GenericMerkleTrie.get[Id, K, V](ByteVector.empty.bits)

      val program = for
        _     <- putEmptyWithEmpty
        _     <- put00_00
        value <- getEmpty
      yield assertions.assertEquals(value, Some(ByteVector.empty))

//    val forPrint = for
//      state1 <- putEmptyWithEmpty.runS(initialState)
//      state2 <- put00_00.runS(state1)
//      state3 <- getEmpty.runS(state2)
//    yield
//      Seq(state1, state2, state3).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }
      program.runA(initialState).value
    }
  }

  test("put 00 -> put 0000 -> put empty -> get empty") {
    withMunitAssertions { assertions =>
      val initialState = GenericMerkleTrieState.empty[K, V]
      val put00   = GenericMerkleTrie.put[Id, K, V](hex"00".bits, ByteVector.empty)
      val put0000 = GenericMerkleTrie.put[Id, K, V](hex"0000".bits, ByteVector.empty)
      val putEmpty =
        GenericMerkleTrie.put[Id, K, V](ByteVector.empty.bits, ByteVector.empty)
      val getEmpty = GenericMerkleTrie.get[Id, K, V](ByteVector.empty.bits)

      val program = for
        _     <- put00
        _     <- put0000
        _     <- putEmpty
        value <- getEmpty
      yield assertions.assertEquals(value, Some(ByteVector.empty))

//    val forPrint = for
//      state1 <- put00.runS(initialState)
//      state2 <- put0000.runS(state1)
//      state3 <- putEmpty.runS(state2)
//    yield
//      Seq(state1, state2, state3).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      program.runA(initialState).value
    }
  }

  test("put 0700 -> put 07 -> put 10 -> get empty") {
    withMunitAssertions { assertions =>
      val initialState = GenericMerkleTrieState.empty[K, V]
      val put0700  = GenericMerkleTrie.put[Id, K, V](hex"0700".bits, ByteVector.empty)
      val put07    = GenericMerkleTrie.put[Id, K, V](hex"07".bits, ByteVector.empty)
      val put10    = GenericMerkleTrie.put[Id, K, V](hex"10".bits, ByteVector.empty)
      val getEmpty = GenericMerkleTrie.get[Id, K, V](ByteVector.empty.bits)

      val program = for
        _     <- put0700
        _     <- put07
        _     <- put10
        value <- getEmpty
      yield assertions.assertEquals(value, None)

//    val forPrint = for
//      state1 <- put0700.runS(initialState)
//      state2 <- put07.runS(state1)
//      state3 <- put10.runS(state2)
//    yield
//      Seq(state1, state2, state3).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      program.runA(initialState).value
    }
  }

  test("put 00 -> put 01 -> get 00") {
    withMunitAssertions { assertions =>
      val initialState = GenericMerkleTrieState.empty[K, V]
      val put00 = GenericMerkleTrie.put[Id, K, V](hex"00".bits, ByteVector.empty)
      val put01 = GenericMerkleTrie.put[Id, K, V](hex"01".bits, ByteVector.empty)
      val get00 = GenericMerkleTrie.get[Id, K, V](hex"00".bits)

      val program = for
        _     <- put00
        _     <- put01
        value <- get00
      yield assertions.assertEquals(value, Some(ByteVector.empty))

//    val forPrint = for
//      state1 <- put00.runS(initialState)
//      state2 <- put01.runS(state1)
//    yield
//      Seq(state1, state2).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      program.runA(initialState).value
    }
  }

  test("put(00, empty) -> put(01, empty) -> put(00, 00) -> get 01") {
    withMunitAssertions { assertions =>
      val initialState = GenericMerkleTrieState.empty[K, V]
      val put00    = GenericMerkleTrie.put[Id, K, V](hex"00".bits, ByteVector.empty)
      val put01    = GenericMerkleTrie.put[Id, K, V](hex"01".bits, ByteVector.empty)
      val put00_00 = GenericMerkleTrie.put[Id, K, V](hex"00".bits, hex"00")
      val get01    = GenericMerkleTrie.get[Id, K, V](hex"01".bits)

      val program = for
        _     <- put00
        _     <- put01
        _     <- put00_00
        value <- get01
      yield value

//    val forPrint = for
//      state1 <- put00.runS(initialState)
//      state2 <- put01.runS(state1)
//      state3 <- put00_00.runS(state2)
//    yield
//      Seq(state1, state2, state3).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      val result = program.runA(initialState).value
      assertions.assertEquals(result, Right(Some(ByteVector.empty)))
    }
  }

  test("put 50 -> put 5000 -> remove 00") {
    withMunitAssertions { assertions =>
      val initialState = GenericMerkleTrieState.empty[K, V]

      def put(key: ByteVector) =
        GenericMerkleTrie.put[Id, K, V](key.bits, ByteVector.empty)
      def remove(key: ByteVector) = GenericMerkleTrie.remove[Id, K, V](key.bits)

      val program = for
        _      <- put(hex"50")
        _      <- put(hex"5000")
        result <- remove(hex"00")
      yield result

//    val forPrint = for
//      state1 <- put(hex"50").runS(initialState)
//      state2 <- put(hex"5000").runS(state1)
//      result <- remove(hex"00").run(state2)
//    yield
//      Seq(state1, state2, result._1).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }
//      println(s"result: ${result._2}")

      val result = program.runA(initialState).value
      assertions.assertEquals(result, Right(false))
    }
  }

  test("put d0 -> put d000 -> put empty -> put 000000 -> remove d000") {
    withMunitAssertions { assertions =>
      val initialState = GenericMerkleTrieState.empty[K, V]

      def put(key: ByteVector) =
        GenericMerkleTrie.put[Id, K, V](key.bits, ByteVector.empty)
      def remove(key: ByteVector) = GenericMerkleTrie.remove[Id, K, V](key.bits)

      val program = for
        _ <- put(hex"d0")
        _ <- put(hex"d000")
        _ <- put(hex"")
        _ <- put(hex"000000")
        _ <- remove(hex"d000")
      yield ()

//    val forPrint = for
//      state1 <- put(hex"d0").runS(initialState)
//      state2 <- put(hex"d000").runS(state1)
//      state3 <- put(hex"").runS(state2)
//      state4 <- put(hex"000000").runS(state3)
//      state5 <- remove(hex"d000").runS(state4)
//    yield
//      Seq(state1, state2, state3, state4).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      val result = program.runA(initialState).value
      assertions.assertEquals(result, Right(()))
    }
  }

  property("test merkle trie") {
    sequential(
      range = Range.linear(1, 100),
      initial = State.empty,
      commands = List(commandGet, commandPut, commandRemove, commandFrom),
      cleanup = () => merkleTrieState = GenericMerkleTrieState.empty[K, V],
    )
  }