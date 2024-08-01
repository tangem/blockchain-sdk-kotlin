package com.tangem.blockchain.blockchains.mantle

import com.tangem.blockchain.blockchains.ethereum.CompiledEthereumTransaction
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import java.math.BigDecimal

class MantleWalletManager(
    wallet: Wallet,
    transactionBuilder: EthereumTransactionBuilder,
    networkProvider: EthereumNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider = DefaultTransactionHistoryProvider,
) : EthereumWalletManager(wallet, transactionBuilder, networkProvider, transactionHistoryProvider) {

    override suspend fun getFeeInternal(amount: Amount, destination: String, data: String?): Result<TransactionFee> {
        val patchedAmount = amount.minus(ONE_WEI)
        return super.getFeeInternal(patchedAmount, destination, data)
            .map {
                when (it) {
                    is TransactionFee.Single -> {
                        val fee = requireEthereumFee(it.normal)
                        it.copy(normal = mapFeeForEstimate(fee))
                    }
                    is TransactionFee.Choosable -> {
                        val normalFee = requireEthereumFee(it.normal)
                        val minimumFee = requireEthereumFee(it.minimum)
                        val priorityFee = requireEthereumFee(it.priority)

                        TransactionFee.Choosable(
                            normal = mapFeeForEstimate(normalFee),
                            minimum = mapFeeForEstimate(minimumFee),
                            priority = mapFeeForEstimate(priorityFee),
                        )
                    }
                }
            }
    }

    override suspend fun sign(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<Pair<ByteArray, CompiledEthereumTransaction>> {
        return when (transactionData) {
            is TransactionData.Uncompiled -> {
                val fee = requireEthereumFee(transactionData.fee)

                val patchedFee = fee.copy(
                    amount = fee.amount.copy(value = fee.amount.value?.multiply(FEE_SEND_MULTIPLIER)),
                    gasLimit = fee.gasLimit.toBigDecimal().multiply(FEE_SEND_MULTIPLIER).toBigInteger(),
                )

                super.sign(
                    transactionData = transactionData.copy(
                        amount = transactionData.amount.minus(ONE_WEI),
                        fee = patchedFee,
                        // Presence of extras is a workaround for correct gasPrice calculation in EthereumUtils
                        // This scenario needs to be reworked
                        // [REDACTED_JIRA]
                        extras = EthereumTransactionExtras(
                            gasLimit = fee.gasLimit.toBigDecimal().multiply(FEE_SEND_MULTIPLIER).toBigInteger(),
                            data = ByteArray(0), // intentionally is zero
                            nonce = null, // intentionally is null
                        ),
                    ),
                    signer = signer,
                )
            }
            is TransactionData.Compiled -> {
                super.sign(transactionData, signer)
            }
        }
    }

    private fun mapFeeForEstimate(fee: Fee.Ethereum): Fee {
        return fee.copy(
            amount = fee.amount.copy(value = fee.amount.value?.multiply(FEE_ESTIMATE_MULTIPLIER)),
            gasLimit = fee.gasLimit.toBigDecimal().multiply(FEE_ESTIMATE_MULTIPLIER).toBigInteger(),
        )
    }

    private fun requireEthereumFee(fee: Fee?): Fee.Ethereum {
        return fee as? Fee.Ethereum ?: error("Fee should be Fee.Ethereum")
    }

    companion object {
        private val FEE_ESTIMATE_MULTIPLIER = BigDecimal("1.6")
        private val FEE_SEND_MULTIPLIER = BigDecimal("0.7")
        private val ONE_WEI = BigDecimal("1E-18")
    }
}