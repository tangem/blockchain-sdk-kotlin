package com.tangem.blockchain.yieldsupply.addressfactory

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.yieldsupply.YieldSupplyProviderType
import org.junit.Test

internal class YieldSupplyContractAddressFactoryTest {

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
                factoryContractAddress = "0xd2806fF93F5776FeA2F0c4863c56669Ec972525d",
                processorContractAddress = "0x9002A6fC37B8f5916C4ddAD92001394C967011D6",
                swapExecutionRegistryAddress = "0x833C25c0D4Cb2A36A70ea8F3dE1D20C5B2973fC3",
                latestImplementationAddress = "0x110E56a8F4bcE93acB0944d6d8FeE5116399A32C",
                providerType = YieldSupplyProviderType.AaveV3,
            ),
        )
    }
}