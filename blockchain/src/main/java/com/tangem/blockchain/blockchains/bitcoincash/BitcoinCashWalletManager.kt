package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.extensions.Result

internal class BitcoinCashWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinCashTransactionBuilder,
    transactionHistoryProvider: TransactionHistoryProvider,
    feesCalculator: BitcoinCashFeesCalculator,
    private val networkProvider: BitcoinNetworkProvider,
) : BitcoinWalletManager(wallet, transactionHistoryProvider, transactionBuilder, networkProvider, feesCalculator) {

    override suspend fun updateInternal() {
        when (val response = networkProvider.getInfo(wallet.address)) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }
}