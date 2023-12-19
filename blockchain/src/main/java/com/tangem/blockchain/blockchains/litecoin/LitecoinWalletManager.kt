package com.tangem.blockchain.blockchains.litecoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.extensions.Result

class LitecoinWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinTransactionBuilder,
    networkProvider: BitcoinNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider,
) : BitcoinWalletManager(wallet, transactionHistoryProvider, transactionBuilder, networkProvider), TransactionSender {

    private val addressToEstimateFee = "ltc1qtcl2c7mnld8pk7kjyef9gd2d7hxxz9wqh6ylef"

    override suspend fun estimateFee(amount: Amount): Result<TransactionFee> {
        return getFee(amount, addressToEstimateFee)
    }

    override val minimalFee = 0.00001.toBigDecimal()
}