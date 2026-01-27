package com.tangem.blockchain.blockchains.ethereum.txbuilder

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isZeroAddress
import com.tangem.blockchain.common.*

/**
 * Validator for Ethereum transactions.
 */
class EthereumTransactionValidator(
    val blockchain: Blockchain,
) : TransactionValidator {
    override suspend fun validate(transactionData: TransactionData): Result<Unit> = when (transactionData) {
        is TransactionData.Uncompiled -> validateUncompiledTransaction(transactionData)
        is TransactionData.Compiled -> {
            // Compiled transactions are assumed to be valid
            kotlin.Result.success(Unit)
        }
    }

    private fun validateUncompiledTransaction(transactionData: TransactionData.Uncompiled): Result<Unit> {
        val amount = transactionData.amount
        val destinationAddress = transactionData.destinationAddress
        val extras = transactionData.extras as? EthereumTransactionExtras

        // Validate destination address and ensure it's not the zero address
        val isValidAddress = blockchain.validateAddress(transactionData.destinationAddress)
        val isZeroAddress = destinationAddress.isZeroAddress()
        if (!isValidAddress || isZeroAddress) {
            return kotlin.Result.failure(BlockchainSdkError.FailedToSendException)
        }

        // Validate extras based on amount type
        val areExtrasValid = when (amount.type) {
            AmountType.Coin -> if (extras?.callData != null) {
                extras.callData.validate(blockchain)
            } else {
                true
            }

            // For TokenYieldSupply and Token amount types, validate call data if present
            is AmountType.TokenYieldSupply,
            is AmountType.Token,
            -> extras != null && extras.callData?.validate(blockchain) == true
            else -> true
        }

        // Validate that fee is provided
        val feeCheck = transactionData.fee != null

        // Final validation result
        if (!feeCheck || !areExtrasValid) {
            return kotlin.Result.failure(BlockchainSdkError.FailedToSendException)
        }

        return kotlin.Result.success(Unit)
    }
}