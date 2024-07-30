package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.ducatus.DucatusFeesCalculator
import com.tangem.blockchain.blockchains.ducatus.DucatusWalletManager
import com.tangem.blockchain.blockchains.ducatus.network.DucatusNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory

internal object DucatusWalletManagerAssembly : WalletManagerAssembly<DucatusWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): DucatusWalletManager {
        return with(input) {
            val blockchain = wallet.blockchain
            DucatusWalletManager(
                wallet = wallet,
                transactionBuilder = BitcoinTransactionBuilder(wallet.publicKey.blockchainKey, blockchain),
                networkProvider = DucatusNetworkService(),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                feesCalculator = DucatusFeesCalculator(blockchain),
            )
        }
    }
}