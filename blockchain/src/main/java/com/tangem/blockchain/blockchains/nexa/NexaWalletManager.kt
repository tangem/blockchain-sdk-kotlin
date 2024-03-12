package com.tangem.blockchain.blockchains.nexa

import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashTransactionBuilder
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider

class NexaWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinCashTransactionBuilder, // TODO
    transactionHistoryProvider: TransactionHistoryProvider, // TODO
    private val networkProvider: BitcoinNetworkProvider, // TODO
) : BitcoinWalletManager(wallet, transactionHistoryProvider, transactionBuilder, networkProvider)