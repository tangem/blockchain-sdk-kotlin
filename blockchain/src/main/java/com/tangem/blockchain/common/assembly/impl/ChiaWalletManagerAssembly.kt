package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.chia.ChiaTransactionBuilder
import com.tangem.blockchain.blockchains.chia.ChiaWalletManager
import com.tangem.blockchain.blockchains.chia.network.ChiaNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object ChiaWalletManagerAssembly : WalletManagerAssembly<ChiaWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): ChiaWalletManager {
        return ChiaWalletManager(
            wallet = input.wallet,
            networkProvider = ChiaNetworkService(
                input.wallet.blockchain.isTestnet(),
                input.config.chiaFireAcademyApiKey ?: error("FireAcademy API key not provided")
            ),
            transactionBuilder = ChiaTransactionBuilder(input.wallet.publicKey.blockchainKey, input.wallet.blockchain)
        )
    }
}