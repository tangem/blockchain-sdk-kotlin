package com.tangem.blockchain.extensions

import junit.framework.TestCase.assertTrue
import org.junit.Test

class ByteArrayKtTest {

    @Test
    fun removeLeadingZero_empty_array() {
        val expected = byteArrayOf()

        val data = byteArrayOf()
        val result = data.removeLeadingZero()

        assertTrue(result.contentEquals(expected))
    }

    @Test
    fun removeLeadingZero_array_one_zero_item() {
        val expected = byteArrayOf(0)

        val data = byteArrayOf(0)
        val result = data.removeLeadingZero()

        assertTrue(result.contentEquals(expected))
    }

    @Test
    fun removeLeadingZero() {
        val expected = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9)

        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val result = data.removeLeadingZero()

        assertTrue(result.contentEquals(expected))
    }
}