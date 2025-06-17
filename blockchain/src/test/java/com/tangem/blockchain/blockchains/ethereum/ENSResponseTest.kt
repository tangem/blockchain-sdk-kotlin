package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.converters.ENSResponseConverter
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ENSResponseTest {

    @Test
    fun test1() {
        val result = "0x" +
            "0000000000000000000000000000000000000000000000000000000000000040" +
            "000000000000000000000000231b0ee14048e9dccd1d247744d114a4eb5e8e63" +
            "0000000000000000000000000000000000000000000000000000000000000020" +
            "000000000000000000000000d8da6bf26964af9d7eed9e03e53415d37aa96045"

        val expected = "0xd8da6bf26964af9d7eed9e03e53415d37aa96045"
        val actual = ENSResponseConverter.convert(result)
        assertEquals(expected, actual)
    }

    @Test
    fun test2() {
        val result = "0x" +
            "0000000000000000000000000000000000000000000000000000000000000030" +
            "4048e9dccd1d247744d114a4eb5e8e63" +
            "0000000000000000000000000000000000000000000000000000000000000020" +
            "000000000000000000000000d8da6bf26964af9d7eed9e03e53415d37aa96045"

        val expected = "0xd8da6bf26964af9d7eed9e03e53415d37aa96045"
        val actual = ENSResponseConverter.convert(result)
        assertEquals(expected, actual)
    }

    @Test
    fun test3() {
        val result = "0x" +
            "0000000000000000000000000000000000000000000000000000000000000030" +
            "4048e9dccd1d247744d114a4eb5e8e63" +
            "000000000000000000000000000000000000000000000000000000000000001E" +
            "00000000000000000000d8da6bf26964af9d7eed9e03e53415d37aa96045"

        val expected = "0xd8da6bf26964af9d7eed9e03e53415d37aa96045"
        val actual = ENSResponseConverter.convert(result)
        assertEquals(expected, actual)
    }
}