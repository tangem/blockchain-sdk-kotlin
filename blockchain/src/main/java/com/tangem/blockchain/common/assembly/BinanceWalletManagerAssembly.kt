package com.tangem.blockchain.common.assembly

import com.tangem.blockchain.blockchains.binance.BinanceTransactionBuilder
import com.tangem.blockchain.blockchains.binance.BinanceWalletManager
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkService
import com.tangem.blockchain.common.Blockchain

object BinanceWalletManagerAssembly : WalletManagerAssembly<BinanceWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): BinanceWalletManager {
        with(input.wallet) {
            val isTestNet = blockchain == Blockchain.BinanceTestnet
            return BinanceWalletManager(
                this,
                BinanceTransactionBuilder(publicKey.blockchainKey, isTestNet),
                BinanceNetworkService(isTestNet),
                input.presetTokens
            )
        }
    }

}