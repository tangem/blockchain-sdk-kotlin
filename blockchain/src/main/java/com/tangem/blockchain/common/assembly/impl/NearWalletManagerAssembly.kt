package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.near.NearTransactionBuilder
import com.tangem.blockchain.blockchains.near.NearWalletManager
import com.tangem.blockchain.blockchains.near.network.NearJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.near.network.NearNetworkService
import com.tangem.blockchain.blockchains.near.network.api.NearApi
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.createRetrofitInstance

internal object NearWalletManagerAssembly : WalletManagerAssembly<NearWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): NearWalletManager = with(input) {
        val providers = buildList {
            if (wallet.blockchain.isTestnet()) {
                add(createNearJsonRpcProvider(isTestNet = true))
            } else {
                add(createNearJsonRpcProvider(isTestNet = false))
                config.nowNodeCredentials?.apiKey.letNotBlank { add(createNowNodeJsonRpcProvider(it)) }
                config.getBlockCredentials?.apiKey.letNotBlank { add(createGetBlockJsonRpcProvider(it)) }
                // temporarily exclude before AND-5171 fix
                // config.infuraProjectId?.letNotBlank { add(getInfuraProvider(it)) }
            }
        }
        val txBuilder = NearTransactionBuilder(wallet.publicKey)
        val networkService = NearNetworkService(wallet.blockchain, MultiNetworkProvider(providers))

        NearWalletManager(wallet, networkService, txBuilder)
    }

    private fun createNearJsonRpcProvider(isTestNet: Boolean): NearJsonRpcNetworkProvider {
        val url = if (isTestNet) "https://rpc.testnet.near.org/" else "https://rpc.mainnet.near.org/"
        val nearApi = createRetrofitInstance(url).create(NearApi::class.java)
        return NearJsonRpcNetworkProvider(url, nearApi)
    }

    private fun createGetBlockJsonRpcProvider(apiKey: String): NearJsonRpcNetworkProvider {
        val baseUrl = "https://near.getblock.io/"
        val nearApi = createRetrofitInstance("$baseUrl$apiKey/mainnet/").create(NearApi::class.java)
        return NearJsonRpcNetworkProvider(baseUrl, nearApi)
    }

    private fun createNowNodeJsonRpcProvider(apiKey: String): NearJsonRpcNetworkProvider {
        val baseUrl = "https://near.nownodes.io/"
        val nearApi = createRetrofitInstance("$baseUrl$apiKey/").create(NearApi::class.java)
        return NearJsonRpcNetworkProvider(baseUrl, nearApi)
    }

    private fun getInfuraProvider(infuraProjectId: String): NearJsonRpcNetworkProvider {
        val baseUrl = "https://near-mainnet.infura.io/"
        val nearApi = createRetrofitInstance("${baseUrl}v3/").create(NearApi::class.java)
        return NearJsonRpcNetworkProvider(baseUrl, nearApi, infuraProjectId)
    }

}