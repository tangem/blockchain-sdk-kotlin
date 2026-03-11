package com.tangem.blockchain.blockchains.mantle

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumCompiledTxInfo
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.ceil

/**
 * Mantle wallet manager with custom fee calculation.
 *
 * https://tangem.slack.com/archives/GMXC6PP71/p1719591856597299?thread_ts=1714215815.690169&cid=GMXC6PP71
 */
class MantleWalletManager(
    wallet: Wallet,
    transactionBuilder: EthereumTransactionBuilder,
    networkProvider: EthereumNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider = DefaultTransactionHistoryProvider,
) : EthereumWalletManager(
    wallet = wallet,
    transactionBuilder = transactionBuilder,
    networkProvider = networkProvider,
    transactionHistoryProvider = transactionHistoryProvider,
    supportsENS = false,
) {

    override suspend fun getFeeInternal(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData?,
    ): Result<TransactionFee> {
        val patchedAmount = prepareAdjustedAmount(amount)
        return super.getFeeInternal(patchedAmount, destination, callData)
            .map { transactionFee ->
                when (transactionFee) {
                    is TransactionFee.Single -> {
                        transactionFee.copy(
                            normal = mapFeeWithMultiplier(transactionFee.normal, FEE_ESTIMATE_MULTIPLIER),
                        )
                    }
                    is TransactionFee.Choosable -> {
                        TransactionFee.Choosable(
                            normal = mapFeeWithMultiplier(transactionFee.normal, FEE_ESTIMATE_MULTIPLIER),
                            minimum = mapFeeWithMultiplier(transactionFee.minimum, FEE_ESTIMATE_MULTIPLIER),
                            priority = mapFeeWithMultiplier(transactionFee.priority, FEE_ESTIMATE_MULTIPLIER),
                        )
                    }
                }
            }
    }

    override suspend fun sign(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<Pair<ByteArray, EthereumCompiledTxInfo>> {
        return when (transactionData) {
            is TransactionData.Uncompiled -> {
                val patchedFee = mapFeeWithMultiplier(transactionData.fee, FEE_SEND_MULTIPLIER)
                val extras = transactionData.extras as? EthereumTransactionExtras
                val patchedGasLimit = multiplyGasLimit(
                    getGasLimitFromFee(transactionData.fee),
                    FEE_SEND_MULTIPLIER,
                )

                super.sign(
                    transactionData = transactionData.copy(
                        fee = patchedFee,
                        extras = EthereumTransactionExtras(
                            gasLimit = patchedGasLimit,
                            callData = extras?.callData,
                            nonce = extras?.nonce,
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

    /**
     * Adjusts amount for fee estimation when sending max balance.
     * Subtracts minimal value only when amount equals current balance (sending all funds).
     */
    private fun prepareAdjustedAmount(amount: Amount): Amount {
        if (amount.type !is AmountType.Coin) return amount

        val currentBalance = wallet.amounts[AmountType.Coin]?.value ?: return amount
        val amountValue = amount.value ?: return amount

        val delta = BigDecimal.ONE.movePointLeft(wallet.blockchain.decimals())
        val isMaxAmount = (currentBalance - amountValue).abs() <= delta

        return if (isMaxAmount && amountValue > BigDecimal.ZERO) {
            amount.copy(value = amountValue - delta)
        } else {
            amount
        }
    }

    private fun mapFeeWithMultiplier(fee: Fee?, multiplier: BigDecimal): Fee {
        val ethereumFee = fee as? Fee.Ethereum
            ?: error("Fee should be Fee.Ethereum, but was $fee")

        val newGasLimit = multiplyGasLimit(ethereumFee.gasLimit, multiplier)

        return when (ethereumFee) {
            is Fee.Ethereum.EIP1559 -> {
                val newAmount = calculateFeeAmount(newGasLimit, ethereumFee.maxFeePerGas)
                ethereumFee.copy(
                    amount = ethereumFee.amount.copy(value = newAmount),
                    gasLimit = newGasLimit,
                )
            }
            is Fee.Ethereum.Legacy -> {
                val newAmount = calculateFeeAmount(newGasLimit, ethereumFee.gasPrice)
                ethereumFee.copy(
                    amount = ethereumFee.amount.copy(value = newAmount),
                    gasLimit = newGasLimit,
                )
            }
            is Fee.Ethereum.TokenCurrency -> {
                error("TokenCurrency fee is not supported for Mantle")
            }
        }
    }

    private fun getGasLimitFromFee(fee: Fee?): BigInteger {
        val ethereumFee = fee as? Fee.Ethereum
            ?: error("Fee should be Fee.Ethereum, but was $fee")
        return ethereumFee.gasLimit
    }

    private fun multiplyGasLimit(gasLimit: BigInteger, multiplier: BigDecimal): BigInteger {
        val result = gasLimit.toBigDecimal().multiply(multiplier)
        return ceil(result.toDouble()).toLong().toBigInteger()
    }

    private fun calculateFeeAmount(gasLimit: BigInteger, gasPrice: BigInteger): BigDecimal {
        val decimals = wallet.blockchain.decimals()
        return (gasLimit * gasPrice).toBigDecimal().movePointLeft(decimals)
    }

    companion object {
        private val FEE_ESTIMATE_MULTIPLIER = BigDecimal("1.6")
        private val FEE_SEND_MULTIPLIER = BigDecimal("0.7")
    }
}