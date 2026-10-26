package com.tangem.blockchain.blockchains.onyx

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.toAcceptedCoinValue
import com.tangem.blockchain.common.Blockchain
import org.junit.Test
import java.math.BigInteger

internal class OnyxCoinValueTest {

    @Test
    fun `Onyx drops the sub-tinyxcn remainder`() {
        val wei = BigInteger("193300012345678912")

        Truth.assertThat(Blockchain.Onyx.toAcceptedCoinValue(wei)).isEqualTo(BigInteger("193300010000000000"))
        Truth.assertThat(Blockchain.OnyxTestnet.toAcceptedCoinValue(wei)).isEqualTo(BigInteger("193300010000000000"))
    }

    @Test
    fun `Onyx keeps a whole tinyxcn value`() {
        val wei = BigInteger("193300000000000000")

        Truth.assertThat(Blockchain.Onyx.toAcceptedCoinValue(wei)).isEqualTo(wei)
    }

    @Test
    fun `Onyx turns a value below one tinyxcn into zero`() {
        Truth.assertThat(Blockchain.Onyx.toAcceptedCoinValue(BigInteger("9999999999"))).isEqualTo(BigInteger.ZERO)
    }

    @Test
    fun `other EVM chains keep the value as is`() {
        val wei = BigInteger("193300012345678912")

        Truth.assertThat(Blockchain.Ethereum.toAcceptedCoinValue(wei)).isEqualTo(wei)
        Truth.assertThat(Blockchain.Arc.toAcceptedCoinValue(wei)).isEqualTo(wei)
    }
}