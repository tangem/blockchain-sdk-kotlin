package com.tangem.blockchain.blockchains.dogecoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

internal class DogecoinWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinTransactionBuilder,
    networkProvider: BitcoinNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider,
    feesCalculator: BitcoinFeesCalculator,
) : BitcoinWalletManager(wallet, transactionHistoryProvider, transactionBuilder, networkProvider, feesCalculator),
    TransactionSender {

    override val dustValue: BigDecimal = MINIMAL_FEE_PER_KB

    override suspend fun getBitcoinFeePerKb(): Result<BitcoinFee> {
        return Result.Success(
            BitcoinFee(
                minimalPerKb = MINIMAL_FEE_PER_KB,
                normalPerKb = NORMAL_FEE_PER_KB,
                priorityPerKb = PRIORITY_FEE_PER_KB,
            ),
        )
    }

    private companion object {
        val MINIMAL_FEE_PER_KB = BigDecimal("0.01")
        val NORMAL_FEE_PER_KB = MINIMAL_FEE_PER_KB * BigDecimal("10")
        val PRIORITY_FEE_PER_KB = MINIMAL_FEE_PER_KB * BigDecimal("100")
    }
}