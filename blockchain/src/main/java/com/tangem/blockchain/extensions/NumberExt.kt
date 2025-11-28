package com.tangem.blockchain.extensions

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** return 2 bytes of Long. LittleEndian format */
@Suppress("MagicNumber")
internal fun Long.bytes2LittleEndian(): ByteArray {
    val buffer = ByteBuffer.allocate(2)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    buffer.putShort(toShort())
    return buffer.array()
}

/** return 4 bytes of Long. LittleEndian format */
@Suppress("MagicNumber")
internal fun Int.bytes4LittleEndian(): ByteArray {
    val buffer = ByteBuffer.allocate(4)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(this)
    return buffer.array()
}

/** return 8 bytes of Long. LittleEndian format */
@Suppress("MagicNumber")
internal fun Long.bytes8LittleEndian(): ByteArray {
    val buffer = ByteBuffer.allocate(8)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    buffer.putLong(this)
    return buffer.array()
}

/** return 8 bytes of Long. BigEndian format */
@Suppress("MagicNumber")
internal fun Long.bytes8BigEndian(): ByteArray {
    val buffer = ByteBuffer.allocate(8)
    buffer.order(ByteOrder.BIG_ENDIAN)
    buffer.putLong(this)
    return buffer.array()
}