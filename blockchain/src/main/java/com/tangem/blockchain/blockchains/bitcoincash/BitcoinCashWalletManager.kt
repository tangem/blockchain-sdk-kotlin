package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

class BitcoinCashWalletManager(
        cardId: String,
        wallet: Wallet,
        transactionBuilder: BitcoinCashTransactionBuilder,
        networkProvider: BitcoinNetworkProvider
) : BitcoinWalletManager(cardId, wallet, transactionBuilder, networkProvider), TransactionSender {
    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val minimalFee = BigDecimal("0.00001")
        return when (val result = super.getFee(amount, destination)) {
            is Result.Success -> {
                for (fee in result.data) {
                    if (fee.value!! < minimalFee) fee.value = minimalFee
                }
                result
            }
            is Result.Failure -> result
        }
    }
}