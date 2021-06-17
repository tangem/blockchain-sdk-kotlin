package com.tangem.blockchain.blockchains.dogecoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

class DogecoinWalletManager(
        wallet: Wallet,
        transactionBuilder: BitcoinTransactionBuilder,
        networkProvider: BitcoinNetworkProvider
) : BitcoinWalletManager(wallet, transactionBuilder, networkProvider), TransactionSender {
    override val minimalFeePerKb: BigDecimal = BigDecimal.ONE
    override val minimalFee: BigDecimal = BigDecimal.ONE
    override val dustValue: BigDecimal = BigDecimal.ONE
}