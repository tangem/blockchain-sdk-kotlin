package com.tangem.blockchain.blockchains.chia.clvm

import com.tangem.blockchain.blockchains.chia.extensions.chiaEncode
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toInt
import java.security.InvalidParameterException
import kotlin.experimental.and

/**
 * Logic copied from chia-crypto-utils library
 * https://github.com/irulast/chia-crypto-utils/blob/a07e7457f40e0d243482b6793518956df5b50a84/lib/src/clvm/program.dart
 * https://github.com/irulast/chia-crypto-utils/blob/a07e7457f40e0d243482b6793518956df5b50a84/lib/src/clvm/ir.dart
 */
@Suppress("MagicNumber")
sealed class Program {
    class Atom(val atom: ByteArray) : Program()
    class Cons(val left: Program, val right: Program) : Program()

    fun hash(): ByteArray {
        return when (this) {
            is Atom -> (byteArrayOf(1) + atom).calculateSha256()
            is Cons -> (byteArrayOf(2) + left.hash() + right.hash()).calculateSha256()
        }
    }

    fun serialize(): ByteArray {
        return when (this) {
            is Atom -> {
                when {
                    atom.isEmpty() -> byteArrayOf(0x80.toByte())
                    atom.size == 1 && atom[0].toUByte() <= 0x7Fu -> byteArrayOf(atom[0])
                    else -> {
                        val size = atom.size.toUInt()
                        val result = mutableListOf<Byte>()
                        when {
                            size < 0x40u -> result.add(size.or(0x80u).toByte())
                            size < 0x2000u -> {
                                result.add(size.shr(8).or(0xC0u).toByte())
                                result.add(size.and(0xFFu).toByte())
                            }
                            size < 0x100000u -> {
                                result.add(size.shr(16).or(0xE0u).toByte())
                                result.add(size.shr(8).and(0xFFu).toByte())
                                result.add(size.and(0xFFu).toByte())
                            }
                            size < 0x8000000u -> {
                                result.add(size.shr(24).or(0xF0u).toByte())
                                result.add(size.shr(16).and(0xFFu).toByte())
                                result.add(size.shr(8).and(0xFFu).toByte())
                                result.add(size.and(0xFFu).toByte())
                            }
                            size < 0x400000000u -> {
                                result.add(size.shr(32).or(0xF8u).toByte())
                                result.add(size.shr(24).and(0xFFu).toByte())
                                result.add(size.shr(16).and(0xFFu).toByte())
                                result.add(size.shr(8).and(0xFFu).toByte())
                                result.add(size.and(0xFFu).toByte())
                            }
                            // never happens, previous step covers Int range
                            else -> throw InvalidParameterException("Too many bytes to encode")
                        }

                        result.addAll(atom.toList())
                        result.toByteArray()
                    }
                }
            }
            is Cons -> byteArrayOf(0xff.toByte()) + left.serialize() + right.serialize()
        }
    }

    companion object {
        val NIL = Atom(byteArrayOf())

        fun fromLong(number: Long) = Atom(number.chiaEncode())

        fun fromBytes(bytes: ByteArray) = Atom(bytes)

        fun fromList(list: List<Program>): Program {
            var result: Program = NIL
            for (item in list.reversed()) {
                result = Cons(item, result)
            }
            return result
        }

        fun deserialize(programBytes: ByteArray) = deserialize(programBytes.iterator())

        private fun deserialize(programByteIterator: ByteIterator): Program {
            var sizeBytes: ByteArray

            val currentByte = programByteIterator.nextByte()
            val currentUByte = currentByte.toUByte()
            when {
                currentUByte <= 0x7Fu -> return Atom(byteArrayOf(currentByte))
                currentUByte <= 0xBFu -> sizeBytes = byteArrayOf(currentByte.and(0x3F))
                currentUByte <= 0xDFu -> {
                    sizeBytes = byteArrayOf(currentByte.and(0x1F)) + programByteIterator.nextByte()
                }
                currentUByte <= 0xEFu -> {
                    sizeBytes = byteArrayOf(currentByte.and(0x0F)) + programByteIterator.nextBytes(2)
                }
                currentUByte <= 0xF7u -> {
                    sizeBytes = byteArrayOf(currentByte.and(0x07)) + programByteIterator.nextBytes(3)
                }
                currentUByte <= 0xFBu -> {
                    sizeBytes = byteArrayOf(currentByte.and(0x03)) + programByteIterator.nextBytes(4)
                }
                currentUByte == 0xFFu.toUByte() -> {
                    val left = deserialize(programByteIterator)
                    val right = deserialize(programByteIterator)
                    return Cons(left, right)
                }
                else -> throw InvalidParameterException("Invalid CLVM program encoding")
            }

            val size = sizeBytes.toInt()
            return Atom(programByteIterator.nextBytes(size))
        }
    }
}

private fun ByteIterator.nextBytes(byteCount: Int): ByteArray {
    var result = byteArrayOf()
    repeat(byteCount) { result += this.nextByte() }

    return result
}
