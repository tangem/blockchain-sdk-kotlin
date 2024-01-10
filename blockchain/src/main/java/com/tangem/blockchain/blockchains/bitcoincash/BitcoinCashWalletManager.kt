package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.extensions.Result

class BitcoinCashWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinCashTransactionBuilder,
    transactionHistoryProvider: TransactionHistoryProvider,
    private val networkProvider: BitcoinNetworkProvider,
) : BitcoinWalletManager(wallet, transactionHistoryProvider, transactionBuilder, networkProvider) {

    override val minimalFee = 0.00001.toBigDecimal()

    override suspend fun updateInternal() {
        when (val response = networkProvider.getInfo(wallet.address)) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }
}
