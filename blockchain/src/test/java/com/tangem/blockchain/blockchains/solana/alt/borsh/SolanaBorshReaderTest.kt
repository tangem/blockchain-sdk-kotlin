package com.tangem.blockchain.blockchains.solana.alt.borsh

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class SolanaBorshReaderTest {

    @Test
    fun `reads little endian integers strings and options`() {
        val data = byteArrayOf(
            7, // u8
            1, // bool
            0x10, 0, 0, 0, // u32
            2, 0, 0, 0, 0, 0, 0, 0, // u64
            2, 0, 0, 0, 'h'.code.toByte(), 'i'.code.toByte(), // string
            1, 9, // Some(9)
            0, // None
            2, 0, 0, 0, 5, 6, // vec<u8> [5, 6]
        )
        val reader = SolanaBorshReader(data)

        assertThat(reader.u8()).isEqualTo(7)
        assertThat(reader.bool()).isTrue()
        assertThat(reader.u32()).isEqualTo(16L)
        assertThat(reader.offset()).isEqualTo(2)
        reader.skipString()
        assertThat(reader.option { it.u8() }).isEqualTo(9)
        assertThat(reader.option { it.u8() }).isNull()
        val items = mutableListOf<Int>()
        reader.vector { items += it.u8() }
        assertThat(items).containsExactly(5, 6).inOrder()
        assertThat(reader.isAtEnd).isTrue()
    }

    @Test
    fun `reading past the end throws`() {
        val error = runCatching { SolanaBorshReader(byteArrayOf(1)).u32() }.exceptionOrNull()

        assertThat(error).isInstanceOf(SolanaBorshException::class.java)
    }

    @Test
    fun `invalid option tag throws`() {
        val error = runCatching { SolanaBorshReader(byteArrayOf(2)).option { it.u8() } }.exceptionOrNull()

        assertThat(error).isInstanceOf(SolanaBorshException::class.java)
    }

    @Test
    fun `reader at offset starts from that byte`() {
        val reader = SolanaBorshReader(byteArrayOf(0, 0, 42)).at(2)

        assertThat(reader.u8()).isEqualTo(42)
    }
}