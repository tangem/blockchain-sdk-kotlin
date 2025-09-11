package com.tangem.blockchain.yieldsupply.providers.ethereum.converters

import com.google.common.truth.Truth
import org.junit.Test
import java.math.BigDecimal

/**
 * Test for [EthereumYieldSupplyStatusConverter]
 */
internal class EthereumYieldSupplyStatusConverterTest {

    @Test
    fun `convert test`() {
        val value = "0x0000000000000000000000000000000000000000000000000000000000000001" +
            "0000000000000000000000000000000000000000000000000000000000000001" +
            "0000000000000000000000000000000000000000000000008ac7230489e80000"

        val actualConverted = EthereumYieldSupplyStatusConverter(18).convert(value)

        Truth.assertThat(actualConverted.isActive).isEqualTo(true)
        Truth.assertThat(actualConverted.isInitialized).isEqualTo(true)
        Truth.assertThat(actualConverted.maxNetworkFee).isEqualTo(BigDecimal.TEN.setScale(18))
    }
}