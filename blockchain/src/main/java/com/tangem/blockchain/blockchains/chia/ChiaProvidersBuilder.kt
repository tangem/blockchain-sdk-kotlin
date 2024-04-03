package com.tangem.blockchain.blockchains.chia

import com.tangem.blockchain.blockchains.chia.network.ChiaJsonRpcProvider
import com.tangem.blockchain.blockchains.chia.network.ChiaNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank
import com.tangem.blockchain.network.API_CHIA_FIREACADEMY
import com.tangem.blockchain.network.API_CHIA_FIREACADEMY_TESTNET
import com.tangem.blockchain.network.API_CHIA_TANGEM

internal class ChiaProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<ChiaNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Chia, Blockchain.ChiaTestnet)

    override fun createProviders(blockchain: Blockchain): List<ChiaNetworkProvider> {
        return buildList {
            val isTestnet = blockchain.isTestnet()

            if (!isTestnet) {
                createTangemProvider()?.let(::add)
            }

            createFireAcademyProvider(isTestnet = isTestnet)?.let(::add)
        }
    }

    private fun createTangemProvider(): ChiaNetworkProvider? {
        return config.chiaTangemApiKey?.letNotBlank {
            ChiaJsonRpcProvider(baseUrl = API_CHIA_TANGEM, key = it)
        }
    }

    private fun createFireAcademyProvider(isTestnet: Boolean): ChiaNetworkProvider? {
        return config.chiaFireAcademyApiKey?.letNotBlank {
            ChiaJsonRpcProvider(
                baseUrl = if (isTestnet) API_CHIA_FIREACADEMY_TESTNET else API_CHIA_FIREACADEMY,
                key = config.chiaFireAcademyApiKey,
            )
        }
    }
}