package com.tangem.blockchain.blockchains.ergo

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager

class ErgoWalletManager(wallet: Wallet,
   ) : WalletManager(wallet) {
    override val currentHost: String
        get() = TODO("Not yet implemented")

    override suspend fun update() {
        TODO("Not yet implemented")
    }
}