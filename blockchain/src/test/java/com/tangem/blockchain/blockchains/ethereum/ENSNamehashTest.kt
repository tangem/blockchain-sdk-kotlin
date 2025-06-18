package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.ens.DefaultENSNameProcessor
import com.tangem.blockchain.blockchains.ethereum.ens.ENSNameProcessor
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.assertEquals
import org.junit.Test
import kotlinx.coroutines.test.runTest
import org.komputing.khex.extensions.toNoPrefixHexString

class ENSNamehashTest {

    private val ensNameProcessor: ENSNameProcessor = DefaultENSNameProcessor()

    @Test
    fun test1() = runTest {
        assertEquals(
            "ee6c4522aab0003e8d14cd40a6af439055fd2577951148c14b6cea9a53475835",
            ensNameProcessor.getNamehash("vitalik.eth").successOr { ByteArray(0) }.toNoPrefixHexString(),
        )
    }

    @Test
    fun test2() = runTest {
        assertEquals(
            "4e34d3a81dc3a20f71bbdf2160492ddaa17ee7e5523757d47153379c13cb46df",
            ensNameProcessor.getNamehash("ens.eth").successOr { ByteArray(0) }.toNoPrefixHexString(),
        )
    }

    @Test
    fun test3() = runTest {
        assertEquals(
            "77872462a9b85d1988752518a048a2b55cb16623ff699e99f3f7ad5b4507c82c",
            ensNameProcessor.getNamehash("test1.test").successOr { ByteArray(0) }.toNoPrefixHexString(),
        )
    }

    @Test
    fun `should fail on empty input`() = runTest {
        val result = ensNameProcessor.getNamehash("")
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `should fail on leading dot`() = runTest {
        val result = ensNameProcessor.getNamehash(".eth")
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `should fail on invalid character`() = runTest {
        val result = ensNameProcessor.getNamehash("ens?.eth")
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `should normalize uppercase`() = runTest {
        assertEquals(
            "4e34d3a81dc3a20f71bbdf2160492ddaa17ee7e5523757d47153379c13cb46df",
            ensNameProcessor.getNamehash("ENS.ETH").successOr { ByteArray(0) }.toNoPrefixHexString(),
        )
    }
}