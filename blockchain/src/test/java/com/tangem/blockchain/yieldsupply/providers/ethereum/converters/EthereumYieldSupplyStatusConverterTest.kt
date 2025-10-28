package com.tangem.blockchain.yieldsupply.providers.ethereum.converters

import com.google.common.truth.Truth
import org.junit.Test
import java.math.BigDecimal

/**
 * Test for [EthereumYieldSupplyStatusConverter]
 */
internal class EthereumYieldSupplyStatusConverterTest {

    @Test
    fun `WHEN valid input THEN return model`() {
        val value = "0x0000000000000000000000000000000000000000000000000000000000000001" +
            "0000000000000000000000000000000000000000000000000000000000000001" +
            "0000000000000000000000000000000000000000000000008ac7230489e80000"

        val actualConverted = EthereumYieldSupplyStatusConverter(18).convert(value)

        Truth.assertThat(actualConverted).isNotNull()
        Truth.assertThat(actualConverted?.isActive).isTrue()
        Truth.assertThat(actualConverted?.isInitialized).isTrue()
        Truth.assertThat(actualConverted?.maxNetworkFee).isEqualTo(BigDecimal.TEN.setScale(18))
    }

    @Test
    fun `WHEN empty input THEN return null`() {
        val value = "0x"

        val actualConverted = EthereumYieldSupplyStatusConverter(18).convert(value)

        Truth.assertThat(actualConverted).isNull()
    }

    @Test
    fun `WHEN invalid input THEN return model`() {
        val value = "0x0000000000000000000000000000000000000000000000000000000000000001" +
            "0000000000000000000000000000000000000000000000000"

        val actualConverted = EthereumYieldSupplyStatusConverter(18).convert(value)

        Truth.assertThat(actualConverted).isNull()
        Truth.assertThat(actualConverted?.isActive).isNull()
        Truth.assertThat(actualConverted?.isInitialized).isNull()
        Truth.assertThat(actualConverted?.maxNetworkFee).isNull()
    }
}