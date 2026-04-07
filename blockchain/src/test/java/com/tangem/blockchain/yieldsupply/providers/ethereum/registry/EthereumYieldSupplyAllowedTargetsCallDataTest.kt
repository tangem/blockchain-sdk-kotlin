package com.tangem.blockchain.yieldsupply.providers.ethereum.registry

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class EthereumYieldSupplyAllowedTargetsCallDataTest {
    private val blockchain = Blockchain.Ethereum
    private val targetAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"

    @Test
    fun `Is call data correct`() {
        val callData = EthereumYieldSupplyAllowedTargetsCallData(targetAddress)
        val expected = "0xb8fe8d5f".hexToBytes() +
            "000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd".hexToBytes()
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `Validate call data`() {
        Truth.assertThat(EthereumYieldSupplyAllowedTargetsCallData(targetAddress).validate(blockchain)).isTrue()
        Truth.assertThat(EthereumYieldSupplyAllowedTargetsCallData("").validate(blockchain)).isFalse()
        Truth.assertThat(
            EthereumYieldSupplyAllowedTargetsCallData(EthereumUtils.ZERO_ADDRESS).validate(blockchain),
        ).isFalse()
    }
}