package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class EthereumYieldSupplyUpgradeToAndCallCallDataTest {
    private val blockchain = Blockchain.Ethereum
    private val newImplementation = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
    private val innerCallData = "0xdeadbeef".hexToBytes()

    @Test
    fun `Is call data correct`() {
        val callData = EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = newImplementation,
            callData = innerCallData,
        )
        val expected = "0x4f1ef286".hexToBytes() +
            "000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd".hexToBytes() +
            "0000000000000000000000000000000000000000000000000000000000000040".hexToBytes() +
            "0000000000000000000000000000000000000000000000000000000000000004".hexToBytes() +
            "deadbeef00000000000000000000000000000000000000000000000000000000".hexToBytes()
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `Validate call data`() {
        val valid = EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = newImplementation,
            callData = innerCallData,
        )
        Truth.assertThat(valid.validate(blockchain)).isTrue()

        val invalid = EthereumYieldSupplyUpgradeToAndCallCallData(
            newImplementation = EthereumUtils.ZERO_ADDRESS,
            callData = innerCallData,
        )
        Truth.assertThat(invalid.validate(blockchain)).isFalse()
    }
}