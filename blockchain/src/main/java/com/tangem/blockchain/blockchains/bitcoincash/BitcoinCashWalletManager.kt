package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Wallet

class BitcoinCashWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinCashTransactionBuilder,
    networkProvider: BitcoinNetworkProvider
) : BitcoinWalletManager(wallet, transactionBuilder, networkProvider) {

    override val minimalFee = 0.00001.toBigDecimal()
}