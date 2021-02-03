package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

class BitcoinCashWalletManager(
        cardId: String,
        wallet: Wallet,
        transactionBuilder: BitcoinCashTransactionBuilder,
        networkService: BitcoinNetworkService
) : BitcoinWalletManager(cardId, wallet, transactionBuilder, networkService), TransactionSender {
    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val minimalFee = BigDecimal("0.00001")
        when (val result = super.getFee(amount, destination)) {
            is Result.Success -> {
                for (fee in result.data) {
                    if (fee.value!! < minimalFee) fee.value = minimalFee
                }
                return result
            }
            is Result.Failure -> return result
        }
    }
}