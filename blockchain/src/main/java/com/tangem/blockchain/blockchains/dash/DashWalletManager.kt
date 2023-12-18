package com.tangem.blockchain.blockchains.dash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.extensions.Result

class DashWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinTransactionBuilder,
    networkProvider: BitcoinNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider,
) : BitcoinWalletManager(wallet, transactionHistoryProvider, transactionBuilder, networkProvider), TransactionSender {

    private val addressToEstimateFee = "XcN7YdKmSshcFSkL5vjJfnWUhnH2Uxqbm4"

    override suspend fun estimateFee(amount: Amount): Result<TransactionFee> {
        return getFee(amount, addressToEstimateFee)
    }

}