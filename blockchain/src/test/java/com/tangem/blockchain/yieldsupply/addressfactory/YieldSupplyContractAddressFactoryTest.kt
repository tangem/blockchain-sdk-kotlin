package com.tangem.blockchain.yieldsupply.addressfactory

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderType
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

internal class YieldSupplyContractAddressFactoryTest {

    @Before
    fun setUp() {
        initToggles(isMonadEnabled = true)
    }

    @Test
    fun `every blockchain advertised for yield supply is accepted by the address factory`() {
        val rejected = Blockchain.yieldSupplySupportedBlockchains()
            .filterNot { YieldSupplyContractAddressFactory(it).isSupported() }

        assertThat(rejected).isEmpty()
    }

    @Test
    fun `every blockchain advertised for yield supply resolves its contract addresses`() {
        val unresolved = Blockchain.yieldSupplySupportedBlockchains().filter { blockchain ->
            runCatching { YieldSupplyContractAddressFactory(blockchain).getYieldSupplyContractAddresses() }.isFailure
        }

        assertThat(unresolved).isEmpty()
    }

    @Test
    fun `every mainnet advertised for yield supply carries a swap registry and an implementation`() {
        val incomplete = Blockchain.yieldSupplySupportedBlockchains()
            .filterNot(Blockchain::isTestnet)
            .filter { blockchain ->
                val addresses = YieldSupplyContractAddressFactory(blockchain).getYieldSupplyContractAddresses()

                addresses.swapExecutionRegistryAddress == null || addresses.latestImplementationAddress == null
            }

        assertThat(incomplete).isEmpty()
    }

    @Test
    fun `Monad resolves the Aave V3 addresses of its deployment`() {
        val addresses = YieldSupplyContractAddressFactory(Blockchain.Monad).getYieldSupplyContractAddresses()

        assertThat(addresses).isEqualTo(
            YieldSupplyContractAddresses(
                factoryContractAddress = "0x727fD7209582A4564566C1955E6C56ce9DF5B366",
                processorContractAddress = "0xA564bC954DBC4378e4df8B6fA424bc9DAD728529",
                swapExecutionRegistryAddress = "0xef250e2A35a4F454A9A497A108bA323986cD28B5",
                latestImplementationAddress = "0xaaF16358336a5e292b1D70057f99c3f2e8241C63",
                providerType = YieldSupplyProviderType.AaveV3,
            ),
        )
    }

    @Test
    fun `Monad is not supported while its yield supply toggle is off`() {
        initToggles(isMonadEnabled = false)

        assertThat(YieldSupplyContractAddressFactory(Blockchain.Monad).isSupported()).isFalse()
    }

    private fun initToggles(isMonadEnabled: Boolean) {
        DepsContainer.onInit(
            config = mockk<BlockchainSdkConfig>(relaxed = true),
            featureToggles = BlockchainFeatureToggles(
                isYieldSupplyEnabled = true,
                isMonadEnabled = isMonadEnabled,
            ),
        )
    }
}