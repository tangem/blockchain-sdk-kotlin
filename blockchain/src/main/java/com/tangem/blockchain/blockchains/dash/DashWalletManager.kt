package com.tangem.blockchain.blockchains.dash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider

internal class DashWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinTransactionBuilder,
    networkProvider: BitcoinNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider,
    feesCalculator: BitcoinFeesCalculator,
) : BitcoinWalletManager(wallet, transactionHistoryProvider, transactionBuilder, networkProvider, feesCalculator),
    TransactionSender