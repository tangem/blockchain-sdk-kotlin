package com.tangem.blockchain.blockchains.alephium.source.serde

import com.tangem.blockchain.blockchains.alephium.source.Bytes
import com.tangem.blockchain.blockchains.alephium.source.U256
import com.tangem.blockchain.blockchains.alephium.source.U32
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.isEmpty

@Suppress("MagicNumber", "UnnecessaryParentheses")
internal object CompactInteger {

    /*
     * unsigned integers are encoded with the first two most significant bits denoting the mode:
     * - 0b00: single-byte mode; [0, 2**6)
     * - 0b01: two-byte mode; [0, 2**14)
     * - 0b10: four-byte mode; [0, 2**30)
     * - 0b11: multi-byte mode: [0, 2**536)
     */
    object Unsigned {
        private const val oneByteBound = 0x40 // 0b01000000
        private val twoByteBound = oneByteBound shl 8
        private val fourByteBound = oneByteBound shl (8 * 3)

        fun encode(n: U32): ByteString {
            val array = when {
                n < U32.unsafe(oneByteBound) -> {
                    byteArrayOf((n.v + SingleByte.prefix).toByte())
                }
                n < U32.unsafe(twoByteBound) -> {
                    byteArrayOf(
                        ((n.v shr 8) + TwoByte.prefix).toByte(),
                        n.v.toByte(),
                    )
                }
                n < U32.unsafe(fourByteBound) -> {
                    byteArrayOf(
                        ((n.v shr 24) + FourByte.prefix).toByte(),
                        (n.v shr 16).toByte(),
                        (n.v shr 8).toByte(),
                        n.v.toByte(),
                    )
                }
                else -> {
                    byteArrayOf(
                        MultiByte.prefix.toByte(),
                        (n.v shr 24).toByte(),
                        (n.v shr 16).toByte(),
                        (n.v shr 8).toByte(),
                        n.v.toByte(),
                    )
                }
            }
            return ByteString(array)
        }

        fun encode(n: U256): ByteString {
            return if (n < U256.unsafe(fourByteBound)) {
                encode(U32.unsafe(n.v.toInt()))
            } else {
                val data = n.v.toByteArray().let {
                    if (it[0] == 0x00.toByte()) it.copyOfRange(1, it.size) else it
                }
                val header = ((data.size - 4) + MultiByte.prefix).toByte()
                ByteString(byteArrayOf(header) + data)
            }
        }

        fun decodeU32(bs: ByteString): Result<Staging<U32>> {
            val (mode, body, rest) = ModeUtils.decode(bs).getOrElse { return Result.failure(it) }
            return decodeU32(mode, body, rest)
        }

        private fun decodeU32(mode: Mode, body: ByteString, rest: ByteString): Result<Staging<U32>> {
            return when (mode) {
                is FixedWidth -> {
                    decodeInt(mode, body, rest).map { it.mapValue { U32.unsafe(it) } }
                }
                MultiByte -> {
                    require(body.size >= 5)
                    if (body.size == 5) {
                        val value = Bytes.toIntUnsafe(body.substring(1, body.size))
                        Result.success(Staging(U32.unsafe(value), rest))
                    } else {
                        Result.failure(
                            SerdeError.wrongFormat("Expect 4 bytes int, but got ${body.size - 1} bytes int"),
                        )
                    }
                }
            }
        }

        private fun decodeInt(mode: FixedWidth, body: ByteString, rest: ByteString): Result<Staging<Int>> {
            return when (mode) {
                SingleByte -> {
                    Result.success(Staging(body[0].toInt(), rest))
                }
                TwoByte -> {
                    require(body.size == 2)
                    val value = ((body[0].toInt() and ModeUtils.maskMode) shl 8) or (body[1].toInt() and 0xff)
                    Result.success(Staging(value, rest))
                }
                FourByte -> {
                    require(body.size == 4)
                    val value = ((body[0].toInt() and ModeUtils.maskMode) shl 24) or
                        ((body[1].toInt() and 0xff) shl 16) or
                        ((body[2].toInt() and 0xff) shl 8) or
                        (body[3].toInt() and 0xff)
                    Result.success(Staging(value, rest))
                }
            }
        }

        fun decodeU256(bs: ByteString): Result<Staging<U256>> {
            val (mode, body, rest) = ModeUtils.decode(bs).getOrElse { return Result.failure(it) }
            return decodeU256(mode, body, rest)
        }

        private fun decodeU256(mode: Mode, body: ByteString, rest: ByteString): Result<Staging<U256>> {
            return when (mode) {
                is FixedWidth -> {
                    decodeInt(mode, body, rest).map { it.mapValue { n -> U256.unsafe(n.toUInt().toLong()) } }
                }
                MultiByte -> {
                    U256.from(body.substring(1, body.size))?.let {
                        Result.success(Staging(it, rest))
                    } ?: Result.failure(RuntimeException("Expect U256, but got $body"))
                }
            }
        }
    }

    object Signed {
        private const val signFlag: Int = 0x20 // 0b00100000
        private const val oneByteBound: Int = 0x20 // 0b00100000
        private const val twoByteBound: Int = oneByteBound shl 8
        private const val fourByteBound: Int = oneByteBound shl (8 * 3)

        fun encode(n: Int): ByteString {
            return if (n >= 0) {
                encodePositiveInt(n)
            } else {
                encodeNegativeInt(n)
            }
        }

        private fun encodePositiveInt(n: Int): ByteString {
            val array = when {
                n < oneByteBound -> byteArrayOf((n + SingleByte.prefix).toByte())
                n < twoByteBound -> byteArrayOf(((n shr 8) + TwoByte.prefix).toByte(), n.toByte())
                n < fourByteBound -> byteArrayOf(
                    ((n shr 24) + FourByte.prefix).toByte(),
                    (n shr 16).toByte(),
                    (n shr 8).toByte(),
                    n.toByte(),
                )
                else -> byteArrayOf(
                    MultiByte.prefix.toByte(),
                    (n shr 24).toByte(),
                    (n shr 16).toByte(),
                    (n shr 8).toByte(),
                    n.toByte(),
                )
            }
            return ByteString(array)
        }

        private fun encodeNegativeInt(n: Int): ByteString {
            val array = when {
                n >= -oneByteBound -> byteArrayOf((n xor SingleByte.negPrefix).toByte())
                n >= -twoByteBound -> byteArrayOf(((n shr 8) xor TwoByte.negPrefix).toByte(), n.toByte())
                n >= -fourByteBound -> byteArrayOf(
                    ((n shr 24) xor FourByte.negPrefix).toByte(),
                    (n shr 16).toByte(),
                    (n shr 8).toByte(),
                    n.toByte(),
                )
                else -> byteArrayOf(
                    MultiByte.prefix.toByte(),
                    (n shr 24).toByte(),
                    (n shr 16).toByte(),
                    (n shr 8).toByte(),
                    n.toByte(),
                )
            }
            return ByteString(array)
        }

        fun encode(n: Long): ByteString {
            return if (n >= -0x20000000L && n < 0x20000000L) {
                encode(n.toInt())
            } else {
                ByteString(
                    byteArrayOf(
                        (4 or MultiByte.prefix).toByte(),
                        (n shr 56).toByte(),
                        (n shr 48).toByte(),
                        (n shr 40).toByte(),
                        (n shr 32).toByte(),
                        (n shr 24).toByte(),
                        (n shr 16).toByte(),
                        (n shr 8).toByte(),
                        n.toByte(),
                    ),
                )
            }
        }

        fun decodeInt(bs: ByteString): Result<Staging<Int>> {
            val tuple = ModeUtils.decode(bs).getOrElse { return Result.failure(SerdeError.Other("Failed to decode")) }
            return tuple.let { (mode, body, rest) ->
                decodeInt(mode, body, rest)
            }
        }

        private fun decodeInt(mode: Mode, body: ByteString, rest: ByteString): Result<Staging<Int>> {
            return when (mode) {
                is FixedWidth -> decodeFixedWidthInt(mode, body, rest)
                is MultiByte -> {
                    require(body.size >= 5)
                    if (body.size == 5) {
                        val value = Bytes.toIntUnsafe(body.substring(1, body.size))
                        Result.success(Staging(value, rest))
                    } else {
                        Result.failure(SerdeError.Other("Expect 4 bytes int, but got ${body.size - 1} bytes int"))
                    }
                }
            }
        }

        private fun decodeFixedWidthInt(mode: FixedWidth, body: ByteString, rest: ByteString): Result<Staging<Int>> {
            val isPositive = (body[0].toInt() and signFlag) == 0
            return if (isPositive) {
                decodePositiveInt(mode, body, rest)
            } else {
                decodeNegativeInt(mode, body, rest)
            }
        }

        private fun decodePositiveInt(mode: FixedWidth, body: ByteString, rest: ByteString): Result<Staging<Int>> {
            return when (mode) {
                is SingleByte -> Result.success(Staging(body[0].toInt(), rest))
                is TwoByte -> {
                    require(body.size == 2)
                    val value = ((body[0].toInt() and ModeUtils.maskMode) shl 8) or (body[1].toInt() and 0xff)
                    Result.success(Staging(value, rest))
                }
                is FourByte -> {
                    require(body.size == 4)
                    val value = ((body[0].toInt() and ModeUtils.maskMode) shl 24) or
                        ((body[1].toInt() and 0xff) shl 16) or
                        ((body[2].toInt() and 0xff) shl 8) or
                        (body[3].toInt() and 0xff)
                    Result.success(Staging(value, rest))
                }
            }
        }

        private fun decodeNegativeInt(mode: FixedWidth, body: ByteString, rest: ByteString): Result<Staging<Int>> {
            return when (mode) {
                is SingleByte -> Result.success(Staging(body[0].toInt() or ModeUtils.maskModeNeg, rest))
                is TwoByte -> {
                    require(body.size == 2)
                    val value = ((body[0].toInt() or ModeUtils.maskModeNeg) shl 8) or (body[1].toInt() and 0xff)
                    Result.success(Staging(value, rest))
                }
                is FourByte -> {
                    require(body.size == 4)
                    val value = ((body[0].toInt() or ModeUtils.maskModeNeg) shl 24) or
                        ((body[1].toInt() and 0xff) shl 16) or
                        ((body[2].toInt() and 0xff) shl 8) or
                        (body[3].toInt() and 0xff)
                    Result.success(Staging(value, rest))
                }
            }
        }

        fun decodeLong(bs: ByteString): Result<Staging<Long>> {
            val tuple = ModeUtils.decode(bs)
                .getOrElse { return Result.failure(SerdeError.Other("Failed to decode")) }
            return tuple.let { (mode, body, rest) ->
                decodeLong(mode, body, rest)
            }
        }

        private fun decodeLong(mode: Mode, body: ByteString, rest: ByteString): Result<Staging<Long>> {
            return when (mode) {
                is FixedWidth -> decodeFixedWidthInt(mode, body, rest).map { it.mapValue(Int::toLong) }
                is MultiByte -> {
                    require(body.size >= 5)
                    if (body.size == 9) {
                        val value = Bytes.toLongUnsafe(body.substring(1, body.size))
                        Result.success(Staging(value, rest))
                    } else {
                        Result.failure(SerdeError.Other("Expect 9 bytes long, but got ${body.size - 1} bytes long"))
                    }
                }
            }
        }
    }

    sealed interface Mode {
        val prefix: Int
        val negPrefix: Int
    }

    object ModeUtils {
        const val maskMode: Int = 0x3f
        const val maskRest: Int = 0xc0
        const val maskModeNeg: Int = (0xffffffc0).toInt()

        fun decode(bs: ByteString): Result<Triple<Mode, ByteString, ByteString>> {
            if (bs.isEmpty()) {
                return Result.failure(SerdeError.incompleteData(1, 0))
            }

            return when (bs[0].toInt() and maskRest) {
                SingleByte.prefix -> Result.success(
                    Triple(
                        SingleByte,
                        bs.substring(0, 1), // take(1)
                        bs.substring(1, bs.size), // drop(1)
                    ),
                )
                TwoByte.prefix -> checkSize(bs, 2, TwoByte)
                FourByte.prefix -> checkSize(bs, 4, FourByte)
                else -> checkSize(bs, (bs[0].toInt() and maskMode) + 4 + 1, MultiByte)
            }
        }

        private fun checkSize(
            bs: ByteString,
            expected: Int,
            mode: Mode,
        ): Result<Triple<Mode, ByteString, ByteString>> {
            return if (bs.size >= expected) {
                Result.success(
                    Triple(
                        mode,
                        bs.substring(0, expected), // take(expected)
                        bs.substring(expected, bs.size), // drop(expected)
                    ),
                )
            } else {
                Result.failure(SerdeError.incompleteData(expected, bs.size))
            }
        }
    }

    sealed interface FixedWidth : Mode

    data object SingleByte : FixedWidth {
        override val prefix: Int = 0x00 // 0b00000000
        override val negPrefix: Int = 0xc0 // 0b11000000
    }

    data object TwoByte : FixedWidth {
        override val prefix: Int = 0x40 // 0b01000000
        override val negPrefix: Int = 0x80 // 0b10000000
    }

    data object FourByte : FixedWidth {
        override val prefix: Int = 0x80 // 0b10000000
        override val negPrefix: Int = 0x40 // 0b01000000
    }

    data object MultiByte : Mode {
        override val prefix: Int = 0xc0 // 0b11000000
        override val negPrefix: Int
            get() = throw NotImplementedError("Not needed at all")
    }
}