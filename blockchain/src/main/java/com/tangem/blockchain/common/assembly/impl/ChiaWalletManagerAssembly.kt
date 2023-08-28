package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.chia.ChiaTransactionBuilder
import com.tangem.blockchain.blockchains.chia.ChiaWalletManager
import com.tangem.blockchain.blockchains.chia.network.ChiaJsonRpcProvider
import com.tangem.blockchain.blockchains.chia.network.ChiaNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.network.API_CHIA_FIREACADEMY
import com.tangem.blockchain.network.API_CHIA_FIREACADEMY_TESTNET
import com.tangem.blockchain.network.API_CHIA_TANGEM

internal object ChiaWalletManagerAssembly : WalletManagerAssembly<ChiaWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): ChiaWalletManager {
        return ChiaWalletManager(
            wallet = input.wallet,
            networkProvider = ChiaNetworkService(
                chiaNetworkProviders = getNetworkProviders(input)
            ),
            transactionBuilder = ChiaTransactionBuilder(input.wallet.publicKey.blockchainKey, input.wallet.blockchain)
        )
    }

    private fun getNetworkProviders(input: WalletManagerAssemblyInput): List<ChiaJsonRpcProvider> {
        return buildList {
            val isTestnet = input.wallet.blockchain.isTestnet()
            val fireAcademyBaseUrl = if (isTestnet) {
                API_CHIA_FIREACADEMY_TESTNET
            } else {
                API_CHIA_FIREACADEMY
            }
            val chiaFireAcademyProvider = ChiaJsonRpcProvider(
                baseUrl = fireAcademyBaseUrl,
                key = input.config.chiaFireAcademyApiKey ?: error("FireAcademy API key not provided"),
            )
            if (isTestnet) {
                // testnet only on fireacademy
                add(chiaFireAcademyProvider)
            } else {
                add(
                    ChiaJsonRpcProvider(
                        baseUrl = API_CHIA_TANGEM,
                        key = input.config.chiaTangemApiKey ?: error("FireAcademy API key not provided"),
                    )
                )
                add(chiaFireAcademyProvider)
            }
        }
    }
}