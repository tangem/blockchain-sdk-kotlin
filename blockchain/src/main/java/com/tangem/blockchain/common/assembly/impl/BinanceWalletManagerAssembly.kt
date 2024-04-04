package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.binance.BinanceProvidersBuilder
import com.tangem.blockchain.blockchains.binance.BinanceTransactionBuilder
import com.tangem.blockchain.blockchains.binance.BinanceWalletManager
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object BinanceWalletManagerAssembly : WalletManagerAssembly<BinanceWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): BinanceWalletManager {
        with(input.wallet) {
            val isTestNet = blockchain == Blockchain.BinanceTestnet
            return BinanceWalletManager(
                wallet = this,
                transactionBuilder = BinanceTransactionBuilder(publicKey.blockchainKey, isTestNet),
                networkProvider = BinanceProvidersBuilder.build(blockchain).first(),
            )
        }
    }
}