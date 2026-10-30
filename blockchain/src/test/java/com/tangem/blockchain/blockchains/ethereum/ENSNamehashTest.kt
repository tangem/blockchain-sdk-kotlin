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

    @Test
    fun `should keep eszett instead of folding it to ss`() = runTest {
        assertEquals(
            "fd55a77d433b04957c6fefe7ab85972708f8e0133843dbe7c708a0ab42b6e722",
            ensNameProcessor.getNamehash("straße.eth").successOr { ByteArray(0) }.toNoPrefixHexString(),
        )
    }

    @Test
    fun `should hash unicode labels without punycode`() = runTest {
        mapOf(
            "café.eth" to "a7369e1df22e06ec6d91162508e400d7af475860638f927e6d1085bb0134a74a",
            "вася.eth" to "88dc99b760ed222af61c5b1c0713d42eb8142ffec52f580050a02d408222e1a1",
            "小明.eth" to "85377eeaa80d928d581108c3321494f3af5da57374f332b258d5e0cfab7d7b74",
        ).forEach { (name, expected) ->
            assertEquals(
                name,
                expected,
                ensNameProcessor.getNamehash(name).successOr { ByteArray(0) }.toNoPrefixHexString(),
            )
        }
    }

    @Test
    fun `should normalize uppercase unicode`() = runTest {
        assertEquals(
            ensNameProcessor.getNamehash("café.eth").successOr { ByteArray(0) }.toNoPrefixHexString(),
            ensNameProcessor.getNamehash("CAFÉ.ETH").successOr { ByteArray(0) }.toNoPrefixHexString(),
        )
    }

    @Test
    fun `should accept emoji label`() = runTest {
        assertTrue(ensNameProcessor.getNamehash("\uD83D\uDCA9.eth") is Result.Success)
    }

    @Test
    fun `should fail on punycode label`() = runTest {
        assertTrue(ensNameProcessor.getNamehash("xn--caf-dma.eth") is Result.Failure)
    }

    @Test
    fun `should fail on hyphens at positions 3 and 4`() = runTest {
        assertTrue(ensNameProcessor.getNamehash("ab--c.eth") is Result.Failure)
    }

    @Test
    fun `should fail on zero width joiner between letters`() = runTest {
        assertTrue(ensNameProcessor.getNamehash("a\u200Db.eth") is Result.Failure)
    }

    @Test
    fun `should fail on single label`() = runTest {
        assertTrue(ensNameProcessor.getNamehash("vitalik") is Result.Failure)
    }
}