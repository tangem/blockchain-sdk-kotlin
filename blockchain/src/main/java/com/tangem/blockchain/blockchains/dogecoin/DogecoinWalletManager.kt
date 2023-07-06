package com.tangem.blockchain.blockchains.dogecoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

class DogecoinWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinTransactionBuilder,
    networkProvider: BitcoinNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider,
) : BitcoinWalletManager(wallet, transactionHistoryProvider, transactionBuilder, networkProvider), TransactionSender {

    override suspend fun getBitcoinFeePerKb(): Result<BitcoinFee> {
        return Result.Success(
            BitcoinFee(
                minimalPerKb = BigDecimal(MINIMAL_FEE_PER_KB),
                normalPerKb = BigDecimal(NORMAL_FEE_PER_KB),
                priorityPerKb = BigDecimal(PRIORITY_FEE_PER_KB)
            )
        )
    }

    private companion object {
        const val MINIMAL_FEE_PER_KB = 0.01
        const val NORMAL_FEE_PER_KB = MINIMAL_FEE_PER_KB * 10
        const val PRIORITY_FEE_PER_KB = MINIMAL_FEE_PER_KB * 100
    }
}
