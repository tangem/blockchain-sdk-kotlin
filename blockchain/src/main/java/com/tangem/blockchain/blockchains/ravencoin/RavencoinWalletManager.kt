package com.tangem.blockchain.blockchains.ravencoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Wallet
import java.math.BigDecimal

class RavencoinWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinTransactionBuilder,
    networkProvider: BitcoinNetworkProvider
) : BitcoinWalletManager(wallet, transactionBuilder, networkProvider) {

    // https://github.com/raven-community/ravencore-lib/blob/master/docs/transaction.md
    override val minimalFeePerKb: BigDecimal = BigDecimal(10_000).movePointLeft(wallet.blockchain.decimals())
    override val dustValue: BigDecimal = BigDecimal(642).movePointLeft(wallet.blockchain.decimals())
}