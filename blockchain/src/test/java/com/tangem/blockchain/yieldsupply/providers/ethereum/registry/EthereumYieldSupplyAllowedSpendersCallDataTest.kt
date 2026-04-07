package com.tangem.blockchain.yieldsupply.providers.ethereum.registry

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class EthereumYieldSupplyAllowedSpendersCallDataTest {
    private val blockchain = Blockchain.Ethereum
    private val spenderAddress = "0x1234567890abcdef1234567890abcdef12345678"

    @Test
    fun `Is call data correct`() {
        val callData = EthereumYieldSupplyAllowedSpendersCallData(spenderAddress)
        val expected = "0xd8528af0".hexToBytes() +
            "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes()
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `Validate call data`() {
        Truth.assertThat(EthereumYieldSupplyAllowedSpendersCallData(spenderAddress).validate(blockchain)).isTrue()
        Truth.assertThat(EthereumYieldSupplyAllowedSpendersCallData("").validate(blockchain)).isFalse()
        Truth.assertThat(
            EthereumYieldSupplyAllowedSpendersCallData(EthereumUtils.ZERO_ADDRESS).validate(blockchain),
        ).isFalse()
    }
}