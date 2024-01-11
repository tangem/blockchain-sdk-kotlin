package com.tangem.blockchain.blockchains.vechain

import com.tangem.blockchain.blockchains.vechain.network.VechainApi
import com.tangem.blockchain.blockchains.vechain.network.VechainNetworkProvider
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.createRetrofitInstance

internal class VechainNetworkProvidersBuilder {

    fun build(isTestNet: Boolean, config: BlockchainSdkConfig): List<VechainNetworkProvider> {
        return buildList {
            if (isTestNet) {
                add(createVechainNode("https://testnet.vecha.in/"))
                add(createVechainNode("https://sync-testnet.vechain.org/"))
                add(createVechainNode("https://testnet.veblocks.net/"))
                add(createVechainNode("https://testnetc1.vechain.network/"))
            } else {
                config.nowNodeCredentials?.apiKey.letNotBlank { add(createVechainNode("https://vet.nownodes.io/$it/")) }
                add(createVechainNode("https://mainnet.vecha.in/"))
                add(createVechainNode("https://sync-mainnet.vechain.org/"))
                add(createVechainNode("https://mainnet.veblocks.net/"))
                add(createVechainNode("https://mainnetc1.vechain.network/"))
                add(createVechainNode("https://us.node.vechain.energy/"))
            }
        }
    }

    private fun createVechainNode(url: String): VechainNetworkProvider {
        val vechainApi = createRetrofitInstance(url).create(VechainApi::class.java)
        return VechainNetworkProvider(
            baseUrl = url,
            api = vechainApi,
        )
    }
}