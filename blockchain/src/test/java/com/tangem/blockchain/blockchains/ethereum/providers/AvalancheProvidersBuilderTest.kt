package com.tangem.blockchain.blockchains.ethereum.providers

import com.google.common.truth.Truth
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.common.network.providers.getAllNonpublicProviderTypes
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
class AvalancheProvidersBuilderTest {

    private val allProviderTypes = getAllNonpublicProviderTypes()

    private val blockchainSdkConfig = BlockchainSdkConfig(
        nowNodeCredentials = NowNodeCredentials(apiKey = "apiKey"),
        getBlockCredentials = GetBlockCredentials(
            avalanche = GetBlockAccessToken(jsonRpc = "jsonRpc"),
            xrp = GetBlockAccessToken(),
            cardano = null,
            eth = null,
            etc = null,
            fantom = null,
            rsk = null,
            bsc = null,
            polygon = null,
            gnosis = null,
            cronos = null,
            solana = null,
            ton = null,
            tron = null,
            cosmos = null,
            near = null,
            dogecoin = GetBlockAccessToken(),
            litecoin = GetBlockAccessToken(),
            dash = GetBlockAccessToken(),
            bitcoin = GetBlockAccessToken(),
            aptos = null,
            algorand = null,
            polygonZkEvm = null,
            zkSyncEra = null,
            base = null,
            blast = null,
            filecoin = null,
            sui = null,
        ),
    )

    init {
        DepsContainer.onInit(
            config = blockchainSdkConfig,
            featureToggles = BlockchainFeatureToggles(isEthereumEIP1559Enabled = true),
        )
    }

    @Test
    fun test_provider_types() {
        val publicUrl = "https://avalanche.com/"
        val providerTypes = allProviderTypes + ProviderType.Public(url = publicUrl)

        val actual = AvalancheProvidersBuilder(providerTypes, blockchainSdkConfig).build(Blockchain.Telos)

        Truth.assertThat(actual).hasSize(3)
        Truth.assertThat(actual.map { it.baseUrl })
            .containsExactly("https://go.getblock.io/jsonRpc/", "https://avax.nownodes.io/", publicUrl)
            .inOrder()
    }
}