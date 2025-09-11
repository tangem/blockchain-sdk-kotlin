package com.tangem.blockchain.yieldsupply

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.yieldsupply.providers.EthereumYieldSupplyProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

internal class YieldSupplyProviderFactoryTest {

    private val dataStorage = mockk<AdvancedDataStorage>(relaxed = true)
    private val wallet = mockk<Wallet>(relaxed = true) {
        every { blockchain } returns Blockchain.EthereumTestnet
    }
    val networkProvider = mockk<MultiNetworkProvider<EthereumJsonRpcProvider>>(relaxed = true)

    @Test
    fun `returns DefaultYieldSupplyProvider when isYieldSupplyEnabled is false`() {
        val factory = YieldSupplyProviderFactory(dataStorage)

        DepsContainer.onInit(
            config = mockk<BlockchainSdkConfig>(relaxed = true),
            featureToggles = BlockchainFeatureToggles(false),
        )

        val provider = factory.makeProvider(wallet, networkProvider)

        Truth.assertThat(provider.isSupported()).isFalse()
        Truth.assertThat(provider).isInstanceOf(DefaultYieldSupplyProvider::class.java)
    }

    @Test
    fun `returns EthereumYieldSupplyProvider when isYieldSupplyEnabled is true and blockchain is supported`() {
        val factory = YieldSupplyProviderFactory(dataStorage)

        DepsContainer.onInit(
            config = mockk<BlockchainSdkConfig>(relaxed = true),
            featureToggles = BlockchainFeatureToggles(true),
        )

        val provider = factory.makeProvider(wallet, networkProvider)

        Truth.assertThat(provider.isSupported()).isTrue()
        Truth.assertThat(provider).isInstanceOf(EthereumYieldSupplyProvider::class.java)
    }

    @Test
    fun `returns EthereumYieldSupplyProvider when isYieldSupplyEnabled is true and blockchain is not supported`() {
        val factory = YieldSupplyProviderFactory(dataStorage)

        every { wallet.blockchain } returns Blockchain.Bitcoin

        DepsContainer.onInit(
            config = mockk<BlockchainSdkConfig>(relaxed = true),
            featureToggles = BlockchainFeatureToggles(true),
        )

        val provider = factory.makeProvider(wallet, networkProvider)

        Truth.assertThat(provider.isSupported()).isFalse()
        Truth.assertThat(provider).isInstanceOf(DefaultYieldSupplyProvider::class.java)
    }
}