package com.tangem.blockchain.blockchains.telos

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.common.network.providers.getAllNonpublicProviderTypes
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
class TelosProvidersBuilderTest {

    private val allProviderTypes = getAllNonpublicProviderTypes()

    init {
        DepsContainer.onInit(
            config = BlockchainSdkConfig(),
            featureToggles = BlockchainFeatureToggles(isEthereumEIP1559Enabled = true),
        )
    }

    @Test
    fun test_provider_types() {
        val publicUrl = "https://telos.com/"
        val providerTypes = allProviderTypes + ProviderType.Public(url = publicUrl)

        val actual = TelosProvidersBuilder(providerTypes).build(Blockchain.Telos)

        Truth.assertThat(actual).hasSize(1)
        Truth.assertThat(actual.first().baseUrl).isEqualTo(publicUrl)
    }
}