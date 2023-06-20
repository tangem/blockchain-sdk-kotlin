package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class BitcoinCashWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinCashTransactionBuilder,
    private val networkProvider: BitcoinNetworkProvider
) : BitcoinWalletManager(wallet, transactionBuilder, networkProvider) {

    override val minimalFee = 0.00001.toBigDecimal()

    override suspend fun update() {
        when (val response = networkProvider.getInfo(wallet.address)) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }
}