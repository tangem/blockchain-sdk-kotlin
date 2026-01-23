package com.tangem.blockchain.blockchains.solana.alt.borsh

import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BorshDecoder(val bytes: ByteArray) : AbstractDecoder() {

    private val byteBuffer = ByteBuffer.wrap(bytes).apply {
        order(ByteOrder.LITTLE_ENDIAN) // borsh specification is little endian
    }

    override val serializersModule = EmptySerializersModule()

    // Not called for sequential decoders
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0
    override fun decodeSequentially(): Boolean = true
    override fun decodeNotNullMark(): Boolean = decodeBoolean()

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = decodeInt()
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = decodeByte().toInt()

    override fun decodeBoolean(): Boolean = byteBuffer.get().toInt() != 0
    override fun decodeByte(): Byte = byteBuffer.get()
    override fun decodeShort(): Short = byteBuffer.getShort()
    override fun decodeInt(): Int = byteBuffer.getInt()
    override fun decodeLong(): Long = byteBuffer.getLong()
    override fun decodeFloat(): Float = byteBuffer.getFloat()
    override fun decodeDouble(): Double = byteBuffer.getDouble()
    override fun decodeChar(): Char = byteBuffer.getShort().toInt().toChar()
    override fun decodeString(): String {
        val length = byteBuffer.getInt()
        val bytes = ByteArray(length)
        byteBuffer.get(bytes)
        return bytes.decodeToString().replace("\u0000", "")
    }

    fun remaining(): Int = byteBuffer.remaining()

    fun decodeBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        var i = 0
        repeat(size) {
            bytes[i] = byteBuffer.get()
            i++
        }
        return bytes
    }
}

class BorshEncoder : AbstractEncoder() {

    private val bytes = mutableListOf<Byte>()

    // PRIVATE EXTENSIONS
    // Would love to simplify this into a single catch all function, but that would likely require
    // heavy use of reflection or a manual approach to encoding the bytes for each number primitive.
    private val Short.bytes
        get() =
            byteBuffer(Short.SIZE_BYTES).putShort(this).array()

    private val Int.bytes
        get() =
            byteBuffer(Int.SIZE_BYTES).putInt(this).array()

    private val Long.bytes
        get() =
            byteBuffer(Long.SIZE_BYTES).putLong(this).array()

    private val Float.bytes
        get() =
            byteBuffer(Float.SIZE_BYTES).putFloat(this).array()

    private val Double.bytes
        get() =
            byteBuffer(Double.SIZE_BYTES).putDouble(this).array()

    private val nanException = SerializationException("Invalid Input: cannot encode NaN")

    val borshEncodedBytes get() = bytes.toByteArray()

    override val serializersModule = EmptySerializersModule()

    override fun encodeNull() = encodeByte(0)
    override fun encodeNotNullMark() = encodeBoolean(true)

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = encodeByte(index.toByte())

    override fun encodeByte(value: Byte) {
        bytes.add(value)
    }
    override fun encodeBoolean(value: Boolean) = encodeByte(if (value) 1 else 0)
    override fun encodeShort(value: Short) = encodeBytes(value.bytes)
    override fun encodeInt(value: Int) = encodeBytes(value.bytes)
    override fun encodeLong(value: Long) = encodeBytes(value.bytes)

    override fun encodeFloat(value: Float) = if (value.isNaN()) throw nanException else encodeBytes(value.bytes)

    override fun encodeDouble(value: Double) = if (value.isNaN()) throw nanException else encodeBytes(value.bytes)

    override fun encodeChar(value: Char) = encodeShort(value.code.toShort())
    override fun encodeString(value: String) {
        value.encodeToByteArray().apply {
            encodeInt(size)
            encodeBytes(this)
        }
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return super.beginCollection(descriptor, collectionSize)
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        when (value) {
            is Map<*, *> -> super.encodeSerializableValue(
                serializer,
                value.keys.sortedBy { it.hashCode() }.associateWith { value[it] } as T,
            )
            is Set<*> -> super.encodeSerializableValue(
                serializer,
                value.sortedBy { it.hashCode() } as T,
            )
            else -> super.encodeSerializableValue(serializer, value)
        }
    }

    //region PRIVATE METHODS
    private fun encodeBytes(bytes: ByteArray) = bytes.forEach { b -> encodeByte(b) }

    // borsh specification is little endian
    private fun byteBuffer(numBytes: Int) = ByteBuffer.allocate(numBytes).order(ByteOrder.LITTLE_ENDIAN)
    //endregion
}