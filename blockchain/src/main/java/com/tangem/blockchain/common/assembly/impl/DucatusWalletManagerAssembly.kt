package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.getBitcoinTransactionHistoryProvider
import com.tangem.blockchain.blockchains.ducatus.DucatusWalletManager
import com.tangem.blockchain.blockchains.ducatus.network.DucatusNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object DucatusWalletManagerAssembly : WalletManagerAssembly<DucatusWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): DucatusWalletManager {
        return with(input) {
            DucatusWalletManager(
                wallet = wallet,
                transactionBuilder = BitcoinTransactionBuilder(wallet.publicKey.blockchainKey, wallet.blockchain),
                networkProvider = DucatusNetworkService(),
                transactionHistoryProvider = wallet.blockchain.getBitcoinTransactionHistoryProvider(input.config)
            )
        }
    }

}