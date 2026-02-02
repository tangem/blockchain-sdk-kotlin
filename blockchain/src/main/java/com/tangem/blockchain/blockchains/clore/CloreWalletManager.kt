package com.tangem.blockchain.blockchains.clore

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.blockchains.bitcoin.BitcoinMessageSignUtil
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider

internal class CloreWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinTransactionBuilder,
    networkProvider: BitcoinNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider,
    feesCalculator: BitcoinFeesCalculator,
) : BitcoinWalletManager(
    wallet = wallet,
    transactionHistoryProvider = transactionHistoryProvider,
    transactionBuilder = transactionBuilder,
    networkProvider = networkProvider,
    feesCalculator = feesCalculator,
) {
    override val messageMagic: String = BitcoinMessageSignUtil.CLORE_MESSAGE_MAGIC
}