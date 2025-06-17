package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.ens.DefaultENSNameProcessor
import com.tangem.blockchain.blockchains.ethereum.ens.ENSNameProcessor
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.komputing.khex.extensions.toNoPrefixHexString

class ENSEncodeTest {

    private val ensNameProcessor: ENSNameProcessor = DefaultENSNameProcessor()

    @Test
    fun test1() {
        val result = ensNameProcessor.encode("vitalik.eth")
        assertEquals(
            "07766974616c696b0365746800",
            result.getOrThrow().toNoPrefixHexString(),
        )
    }

    @Test
    fun test2() {
        val result = ensNameProcessor.encode("my.name.eth")
        assertEquals(
            "026d79046e616d650365746800",
            result.getOrThrow().toNoPrefixHexString(),
        )
    }
}