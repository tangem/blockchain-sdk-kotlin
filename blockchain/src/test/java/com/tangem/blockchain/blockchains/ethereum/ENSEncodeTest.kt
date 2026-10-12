package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.ens.DefaultENSNameProcessor
import com.tangem.blockchain.blockchains.ethereum.ens.ENSNameProcessor
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.komputing.khex.extensions.toNoPrefixHexString

class ENSEncodeTest {

    private val ensNameProcessor: ENSNameProcessor = DefaultENSNameProcessor()

    @Test
    fun test1() {
        val result = ensNameProcessor.encode("vitalik.eth")
        assertEquals(
            "07766974616c696b0365746800",
            result.successOr { ByteArray(0) }.toNoPrefixHexString(),
        )
    }

    @Test
    fun test2() {
        val result = ensNameProcessor.encode("my.name.eth")
        assertEquals(
            "026d79046e616d650365746800",
            result.successOr { ByteArray(0) }.toNoPrefixHexString(),
        )
    }

    @Test
    fun `should encode label length in utf8 bytes`() {
        val result = ensNameProcessor.encode("вася.eth")
        assertEquals(
            "08d0b2d0b0d181d18f0365746800",
            result.successOr { ByteArray(0) }.toNoPrefixHexString(),
        )
    }

    @Test
    fun `should fail when label exceeds 63 bytes`() {
        val result = ensNameProcessor.encode("${"я".repeat(32)}.eth")
        assertTrue(result is Result.Failure)
    }
}