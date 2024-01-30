package com.tangem.blockchain.blockchains.vechain

import com.tangem.blockchain.blockchains.vechain.network.VeChainApi
import com.tangem.blockchain.blockchains.vechain.network.VeChainNetworkProvider
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.createRetrofitInstance

internal class VeChainNetworkProvidersBuilder {

    fun build(isTestNet: Boolean, config: BlockchainSdkConfig): List<VeChainNetworkProvider> {
        return buildList {
            if (isTestNet) {
                add(createVeChainNode("https://testnet.vecha.in/"))
                add(createVeChainNode("https://sync-testnet.vechain.org/"))
                add(createVeChainNode("https://testnet.veblocks.net/"))
                add(createVeChainNode("https://testnetc1.vechain.network/"))
            } else {
                config.nowNodeCredentials?.apiKey.letNotBlank { add(createVeChainNode("https://vet.nownodes.io/$it/")) }
                add(createVeChainNode("https://mainnet.vecha.in/"))
                add(createVeChainNode("https://sync-mainnet.vechain.org/"))
                add(createVeChainNode("https://mainnet.veblocks.net/"))
                add(createVeChainNode("https://mainnetc1.vechain.network/"))
                add(createVeChainNode("https://us.node.vechain.energy/"))
            }
        }
    }

    private fun createVeChainNode(url: String): VeChainNetworkProvider {
        val veChainApi = createRetrofitInstance(url).create(VeChainApi::class.java)
        return VeChainNetworkProvider(
            baseUrl = url,
            api = veChainApi,
        )
    }
}
