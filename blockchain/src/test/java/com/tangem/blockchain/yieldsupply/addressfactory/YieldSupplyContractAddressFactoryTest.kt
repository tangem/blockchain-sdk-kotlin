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

    @Test
    fun `mainnets resolve V3 processor and implementation while the V3 toggle is on`() {
        initToggles(isMonadEnabled = true, isYieldModuleV3Enabled = true)

        V3_GATED_BLOCKCHAINS.forEach { blockchain ->
            val addresses = YieldSupplyContractAddressFactory(blockchain).getYieldSupplyContractAddresses()

            assertThat(addresses.processorContractAddress).isEqualTo(V3_PROCESSOR_ADDRESS)
            assertThat(addresses.latestImplementationAddress).isEqualTo(V3_IMPLEMENTATION_ADDRESS)
        }
    }

    @Test
    fun `mainnets keep V2 processor and implementation while the V3 toggle is off`() {
        V3_GATED_BLOCKCHAINS.forEach { blockchain ->
            val addresses = YieldSupplyContractAddressFactory(blockchain).getYieldSupplyContractAddresses()

            assertThat(addresses.processorContractAddress).isNotEqualTo(V3_PROCESSOR_ADDRESS)
            assertThat(addresses.latestImplementationAddress).isNotEqualTo(V3_IMPLEMENTATION_ADDRESS)
        }
    }

    @Test
    fun `Ethereum resolves the Aave V3 addresses of its V3 deployment`() {
        initToggles(isMonadEnabled = true, isYieldModuleV3Enabled = true)

        val addresses = YieldSupplyContractAddressFactory(Blockchain.Ethereum).getYieldSupplyContractAddresses()

        assertThat(addresses).isEqualTo(
            YieldSupplyContractAddresses(
                factoryContractAddress = "0xd8972a45616bEC62cB9687e38a99D763c0879B0d",
                processorContractAddress = V3_PROCESSOR_ADDRESS,
                swapExecutionRegistryAddress = "0xF9d772c558743749C91B1A76aA708ae61BC4716c",
                latestImplementationAddress = V3_IMPLEMENTATION_ADDRESS,
                providerType = YieldSupplyProviderType.AaveV3,
            ),
        )
    }

    @Test
    fun `factory and swap registry do not depend on the V3 toggle`() {
        val v2Addresses = V3_GATED_BLOCKCHAINS.associateWith {
            YieldSupplyContractAddressFactory(it).getYieldSupplyContractAddresses()
        }

        initToggles(isMonadEnabled = true, isYieldModuleV3Enabled = true)

        V3_GATED_BLOCKCHAINS.forEach { blockchain ->
            val v3Addresses = YieldSupplyContractAddressFactory(blockchain).getYieldSupplyContractAddresses()
            val v2 = v2Addresses.getValue(blockchain)

            assertThat(v3Addresses.factoryContractAddress).isEqualTo(v2.factoryContractAddress)
            assertThat(v3Addresses.swapExecutionRegistryAddress).isEqualTo(v2.swapExecutionRegistryAddress)
        }
    }

    @Test
    fun `mainnets count both V2 and V3 as V2 or later whatever the V3 toggle`() {
        val toggleOffAddresses = V3_GATED_BLOCKCHAINS.associateWith { blockchain ->
            val factory = YieldSupplyContractAddressFactory(blockchain)
            factory.getV2OrLaterImplementationAddresses() to
                factory.getYieldSupplyContractAddresses().latestImplementationAddress
        }

        initToggles(isMonadEnabled = true, isYieldModuleV3Enabled = true)

        V3_GATED_BLOCKCHAINS.forEach { blockchain ->
            val (v2Addresses, v2Implementation) = toggleOffAddresses.getValue(blockchain)
            val v3Addresses = YieldSupplyContractAddressFactory(blockchain).getV2OrLaterImplementationAddresses()

            assertThat(v3Addresses).isEqualTo(v2Addresses)
            assertThat(v3Addresses).containsExactly(v2Implementation, V3_IMPLEMENTATION_ADDRESS)
        }
    }

    @Test
    fun `Polygon counts its V2 implementation as V2 or later`() {
        val addresses = YieldSupplyContractAddressFactory(
            Blockchain.Polygon,
        ).getV2OrLaterImplementationAddresses()

        assertThat(addresses).contains("0x66084220E3dFdd1D8C8F1F868C103F9418DEce7c")
    }

    @Test
    fun `Monad counts only V3 as V2 or later`() {
        val addresses = YieldSupplyContractAddressFactory(Blockchain.Monad).getV2OrLaterImplementationAddresses()

        assertThat(addresses).containsExactly(V3_IMPLEMENTATION_ADDRESS)
    }

    @Test
    fun `testnet has no V2 or later implementation`() {
        val addresses =
            YieldSupplyContractAddressFactory(Blockchain.EthereumTestnet).getV2OrLaterImplementationAddresses()

        assertThat(addresses).isEmpty()
    }

    private fun initToggles(isMonadEnabled: Boolean, isYieldModuleV3Enabled: Boolean = false) {
        DepsContainer.onInit(
            config = mockk<BlockchainSdkConfig>(relaxed = true),
            featureToggles = BlockchainFeatureToggles(
                isYieldSupplyEnabled = true,
                isMonadEnabled = isMonadEnabled,
                isYieldModuleV3Enabled = isYieldModuleV3Enabled,
            ),
        )
    }

    private companion object {
        const val V3_PROCESSOR_ADDRESS = "0xA564bC954DBC4378e4df8B6fA424bc9DAD728529"
        const val V3_IMPLEMENTATION_ADDRESS = "0xaaF16358336a5e292b1D70057f99c3f2e8241C63"

        val V3_GATED_BLOCKCHAINS = listOf(
            Blockchain.Ethereum,
            Blockchain.Avalanche,
            Blockchain.Arbitrum,
            Blockchain.Optimism,
            Blockchain.Base,
            Blockchain.BSC,
            Blockchain.Polygon,
        )
    }
}