package com.tangem.blockchain.yieldsupply.providers.ethereum.factory

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class EthereumYieldSupplyImplementationCallDataTest {

    @Test
    fun `Is call data correct`() {
        val callData = EthereumYieldSupplyImplementationCallData()
        val expected = "0x5c60da1b".hexToBytes()
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `Validate call data`() {
        Truth.assertThat(EthereumYieldSupplyImplementationCallData().validate(Blockchain.Ethereum)).isTrue()
    }
}