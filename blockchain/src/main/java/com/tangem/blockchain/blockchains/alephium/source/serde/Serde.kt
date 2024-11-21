package com.tangem.blockchain.blockchains.alephium.source.serde

import com.tangem.blockchain.blockchains.alephium.source.Bytes
import com.tangem.blockchain.blockchains.alephium.source.TimeStamp
import com.tangem.blockchain.blockchains.alephium.source.U256
import com.tangem.blockchain.blockchains.alephium.source.U32
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.append
import kotlinx.io.bytestring.buildByteString

internal val boolSerde: Serde<Boolean> = Serde.Companion.BoolSerde
internal val byteSerde: Serde<Byte> = Serde.Companion.ByteSerde
internal val byteArraySerde: Serde<ByteString> = Serde.Companion.ByteStringSerde
internal val intSerde: Serde<Int> = Serde.Companion.IntSerde
internal val u32Serde: Serde<U32> = Serde.Companion.U32Serde
internal val u256Serde: Serde<U256> = Serde.Companion.U256Serde

// for fixedLength
internal fun byteArraySerde(length: Int) = Serde.bytesSerde(length)

internal fun <T> listSerializer(serializer: Serializer<T>) = Serde.Companion.AVectorSerializer(serializer)
internal fun <T> listDeserializer(deserializer: Deserializer<T>) = Serde.Companion.AVectorDeserializer(deserializer)

@Suppress("FunctionNaming")
internal interface Serde<T> : Serializer<T>, Deserializer<T> {

    fun <S> xmap(to: (T) -> S, from: (S) -> T): Serde<S> {
        return object : Serde<S> {
            override fun serialize(input: S): ByteString {
                return this@Serde.serialize(from(input))
            }

            override fun deserialize(input: ByteString): Result<S> {
                return this@Serde.deserialize(input).map(to)
            }

            override fun _deserialize(input: ByteString): Result<Staging<S>> {
                return this@Serde._deserialize(input).map { (t, rest) ->
                    Staging(to(t), rest)
                }
            }
        }
    }

    fun <S> xfmap(to: (T) -> Result<S>, from: (S) -> T): Serde<S> {
        return object : Serde<S> {
            override fun serialize(input: S): ByteString {
                return this@Serde.serialize(from(input))
            }

            override fun deserialize(input: ByteString): Result<S> {
                return this@Serde.deserialize(input).map(to).getOrElse { Result.failure(it) }
            }

            override fun _deserialize(input: ByteString): Result<Staging<S>> {
                return this@Serde._deserialize(input)
                    .map { (t, rest) -> to(t).map { Staging(it, rest) } }
                    .getOrElse { Result.failure(it) }
            }
        }
    }

    fun <S> xomap(to: (T) -> S?, from: (S) -> T): Serde<S> {
        return xfmap(
            { t ->
                to(t)?.let { Result.success(it) } ?: Result.failure(SerdeError.validation("validation error"))
            },
            from,
        )
    }

    fun validate(test: (T) -> Result<Unit>): Serde<T> {
        return object : Serde<T> {
            override fun serialize(input: T): ByteString {
                return this@Serde.serialize(input)
            }

            override fun deserialize(input: ByteString): Result<T> {
                return this@Serde.deserialize(input).map { t ->
                    val result = test(t)
                    result.fold(
                        onSuccess = { Result.success(t) },
                        onFailure = { Result.failure(SerdeError.validation(it.message.orEmpty())) },
                    )
                }.getOrElse { Result.failure(it) }
            }

            override fun _deserialize(input: ByteString): Result<Staging<T>> {
                return this@Serde._deserialize(input).map { (t, rest) ->
                    val result = test(t)
                    result.fold(
                        onSuccess = { Result.success(Staging(t, rest)) },
                        onFailure = { Result.failure(SerdeError.validation(it.message.orEmpty())) },
                    )
                }.getOrElse { Result.failure(it) }
            }
        }
    }

    companion object {

        object BoolSerde : FixedSizeSerde<Boolean> {
            override val serdeSize: Int = Byte.SIZE_BYTES

            override fun serialize(input: Boolean): ByteString {
                return ByteString(if (input) 1 else 0)
            }

            override fun deserialize(input: ByteString): Result<Boolean> {
                return ByteSerde.deserialize(input).map {
                    when (it) {
                        0.toByte() -> Result.success(false)
                        1.toByte() -> Result.success(true)
                        else -> Result.failure(SerdeError.validation("Invalid bool from byte $it"))
                    }
                }.getOrElse { Result.failure(it) }
            }
        }

        object ByteSerde : FixedSizeSerde<Byte> {
            override val serdeSize: Int = Byte.SIZE_BYTES

            override fun serialize(input: Byte): ByteString {
                return ByteString(input)
            }

            override fun deserialize(input: ByteString): Result<Byte> {
                return deserialize0(input) { it[0] }
            }
        }

        object IntSerde : Serde<Int> {
            override fun serialize(input: Int): ByteString {
                return CompactInteger.Signed.encode(input)
            }

            override fun _deserialize(input: ByteString): Result<Staging<Int>> {
                return CompactInteger.Signed.decodeInt(input)
            }
        }

        object LongSerde : Serde<Long> {
            override fun serialize(input: Long): ByteString {
                return CompactInteger.Signed.encode(input)
            }

            override fun _deserialize(input: ByteString): Result<Staging<Long>> {
                return CompactInteger.Signed.decodeLong(input)
            }
        }

        object U256Serde : Serde<U256> {
            override fun serialize(input: U256): ByteString {
                return CompactInteger.Unsigned.encode(input)
            }

            override fun _deserialize(input: ByteString): Result<Staging<U256>> {
                return CompactInteger.Unsigned.decodeU256(input)
            }
        }

        object U32Serde : Serde<U32> {
            override fun serialize(input: U32): ByteString {
                return CompactInteger.Unsigned.encode(input)
            }

            override fun _deserialize(input: ByteString): Result<Staging<U32>> {
                return CompactInteger.Unsigned.decodeU32(input)
            }
        }

        object ByteStringSerde : Serde<ByteString> {
            override fun serialize(input: ByteString): ByteString {
                return buildByteString {
                    append(IntSerde.serialize(input.size))
                    append(input)
                }
            }

            override fun _deserialize(input: ByteString): Result<Staging<ByteString>> {
                return IntSerde._deserialize(input).map { (size, rest) ->
                    when {
                        size < 0 -> Result.failure(SerdeError.validation("Negative byte string length: $size"))
                        rest.size >= size -> {
                            val value = rest.substring(0, size)
                            val remaining = rest.substring(size, rest.size)
                            Result.success(Staging(value, remaining))
                        }
                        else -> Result.failure(SerdeError.incompleteData(size, rest.size))
                    }
                }.getOrElse { Result.failure(it) }
            }
        }

        object Flags {
            const val none: Int = 0
            const val some: Int = 1
            const val left: Int = 0
            const val right: Int = 1

            val noneB: Byte = none.toByte()
            val someB: Byte = some.toByte()
            val leftB: Byte = left.toByte()
            val rightB: Byte = right.toByte()
        }

        class OptionSerde<T>(private val serde: Serde<T?>) : Serde<T?> {
            override fun serialize(input: T?): ByteString {
                return when (input) {
                    null -> ByteSerde.serialize(Flags.noneB)
                    else -> buildByteString {
                        append(ByteSerde.serialize(Flags.someB))
                        append(serde.serialize(input))
                    }
                }
            }

            override fun _deserialize(input: ByteString): Result<Staging<T?>> {
                return ByteSerde._deserialize(input).map { (flag, rest) ->
                    when (flag) {
                        Flags.noneB -> Result.success(Staging(null, rest))
                        Flags.someB -> serde._deserialize(rest).map { (t, r) -> Staging(t, r) }
                        else -> Result.failure(SerdeError.wrongFormat("Expect 0 or 1 for option flag"))
                    }
                }.getOrElse { Result.failure(it) }
            }
        }

        open class BatchDeserializer<T>(private val deserializer: Deserializer<T>) {

            private fun <C : MutableList<T>> __deserializeSeq(
                rest: ByteString,
                index: Int,
                length: Int,
                builder: () -> C,
            ): Result<Staging<C>> {
                return if (index == length) {
                    Result.success(Staging(builder(), rest))
                } else {
                    deserializer._deserialize(rest).map { (t, tRest) ->
                        builder().add(t)
                        __deserializeSeq(tRest, index + 1, length, builder)
                    }.getOrElse { Result.failure(it) }
                }
            }

            fun <C : MutableList<T>> _deserializeSeq(
                size: Int,
                input: ByteString,
                builder: () -> C,
            ): Result<Staging<C>> {
                return __deserializeSeq(input, 0, size, builder)
            }

            private fun _deserializeArray(rest: ByteString, index: Int, output: Array<T>): Result<Staging<Array<T>>> {
                return if (index == output.size) {
                    Result.success(Staging(output, rest))
                } else {
                    deserializer._deserialize(rest).map { (t, tRest) ->
                        output[index] = t
                        _deserializeArray(tRest, index + 1, output)
                    }.getOrElse { Result.failure(it) }
                }
            }

            fun _deserializeArray(n: Int, input: ByteString): Result<Staging<Array<T>>> {
                return when {
                    n < 0 -> Result.failure(SerdeError.validation("Negative array size: $n"))
                    n > input.size -> Result.failure(SerdeError.validation("Malicious array size: $n"))
                    else -> _deserializeArray(input, 0, arrayOfNulls<Any>(n) as Array<T>)
                }
            }

            fun _deserializeAVector(n: Int, input: ByteString): Result<Staging<List<T>>> {
                return _deserializeArray(n, input).map { staging ->
                    Staging(staging.value.toList(), staging.rest)
                }
            }
        }

        fun bytesSerde(length: Int): Serde<ByteString> = object : FixedSizeSerde<ByteString> {
            override val serdeSize: Int = length

            override fun serialize(input: ByteString): ByteString {
                require(input.size == serdeSize) { "Input size must match fixed size" }
                return input
            }

            override fun deserialize(input: ByteString): Result<ByteString> {
                return deserialize0(input) { it }
            }
        }

        class AVectorSerializer<T>(private val serializer: Serializer<T>) : Serializer<List<T>> {
            override fun serialize(input: List<T>): ByteString {
                val serializedLength = IntSerde.serialize(input.size)
                return buildByteString {
                    append(serializedLength)
                    input.forEach { append(serializer.serialize(it)) }
                }
            }
        }

        class AVectorDeserializer<T>(private val deserializer: Deserializer<T>) :
            BatchDeserializer<T>(deserializer),
            Deserializer<List<T>> {
            override fun _deserialize(input: ByteString): Result<Staging<List<T>>> {
                return IntSerde._deserialize(input).map { staging ->
                    val (size, rest) = staging
                    _deserializeAVector(size, rest)
                }.getOrElse { Result.failure(it) }
            }
        }

        fun <T> avectorSerde(serde: Serde<T>): Serde<List<T>> {
            return object : BatchDeserializer<T>(serde), Serde<List<T>> {
                val aVectorSerializer = AVectorSerializer(serde)
                val aVectorDeserializer = AVectorDeserializer(serde)

                override fun serialize(input: List<T>): ByteString = aVectorSerializer.serialize(input)
                override fun _deserialize(input: ByteString): Result<Staging<List<T>>> =
                    aVectorDeserializer._deserialize(input)
            }
        }

        object TimeStampSerde : FixedSizeSerde<TimeStamp> {
            override val serdeSize: Int = TimeStamp.byteLength

            override fun serialize(input: TimeStamp): ByteString {
                return Bytes.from(input.millis)
            }

            override fun deserialize(input: ByteString): Result<TimeStamp> {
                return deserialize1(input) {
                    TimeStamp.from(Bytes.toLongUnsafe(input))
                        ?.let { Result.success(it) }
                        ?: Result.failure(SerdeError.validation("Negative timestamp"))
                }
            }
        }
    }
}

internal interface FixedSizeSerde<T> : Serde<T> {
    val serdeSize: Int

    fun deserialize0(input: ByteString, f: (ByteString) -> T): Result<T> {
        return when {
            input.size == serdeSize -> Result.success(f(input))
            input.size > serdeSize -> Result.failure(SerdeError.redundant(serdeSize, input.size))
            else -> Result.failure(SerdeError.incompleteData(serdeSize, input.size))
        }
    }

    fun deserialize1(input: ByteString, f: (ByteString) -> Result<T>): Result<T> {
        return when {
            input.size == serdeSize -> f(input)
            input.size > serdeSize -> Result.failure(SerdeError.redundant(serdeSize, input.size))
            else -> Result.failure(SerdeError.incompleteData(serdeSize, input.size))
        }
    }

    override fun _deserialize(input: ByteString): Result<Staging<T>> {
        return if (input.size >= serdeSize) {
            val init = input.substring(0, serdeSize)
            val rest = input.substring(serdeSize, input.size)
            deserialize(init).map { Staging(it, rest) }
        } else {
            Result.failure(SerdeError.incompleteData(serdeSize, input.size))
        }
    }
}

internal data class Tuple2<A0, A1>(
    val a0: A0,
    val a1: A1,
)

internal fun <A0, A1, T> forProduct2(
    pack: (A0, A1) -> T,
    unpack: (T) -> Tuple2<A0, A1>,
    serdeA0: Serde<A0>,
    serdeA1: Serde<A1>,
): Serde<T> {
    return object : Serde<T> {
        override fun serialize(input: T): ByteString {
            val (a0, a1) = unpack(input)
            return buildByteString {
                append(serdeA0.serialize(a0))
                append(serdeA1.serialize(a1))
            }
        }

        override fun _deserialize(input: ByteString): Result<Staging<T>> {
            val pair0 = serdeA0._deserialize(input).getOrElse { return Result.failure(it) }
            val pair1 = serdeA1._deserialize(pair0.rest).getOrElse { return Result.failure(it) }

            val value = pack(pair0.value, pair1.value)
            return Result.success(Staging(value, pair1.rest))
        }
    }
}

internal fun <A0, A1> tuple2(serdeA0: Serde<A0>, serdeA1: Serde<A1>): Serde<Pair<A0, A1>> {
    return object : Serde<Pair<A0, A1>> {
        override fun serialize(input: Pair<A0, A1>): ByteString {
            val (a0, a1) = input
            return buildByteString {
                append(serdeA0.serialize(a0))
                append(serdeA1.serialize(a1))
            }
        }

        override fun _deserialize(input: ByteString): Result<Staging<Pair<A0, A1>>> {
            val pair0 = serdeA0._deserialize(input).getOrElse { return Result.failure(it) }
            val pair1 = serdeA1._deserialize(pair0.rest).getOrElse { return Result.failure(it) }
            return Result.success(Staging(pair0.value to pair1.value, pair1.rest))
        }
    }
}