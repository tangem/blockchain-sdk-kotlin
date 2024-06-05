package com.tangem.blockchain.blockchains.nexa

import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.successOr
import java.math.RoundingMode

internal class NexaWalletManager(
    wallet: Wallet,
    private val networkProvider: ElectrumNetworkProvider,
) : WalletManager(wallet) {
    override val currentHost: String
        get() = networkProvider.baseUrl

    private val addressScriptHash: String by lazy {
        NexaAddressService.getScriptHash(wallet.publicKey.blockchainKey)
    }

    override suspend fun updateInternal() {
        val accountRes = networkProvider.getAccountBalance(addressScriptHash)

        val account = accountRes.successOr { throw it.error }

        wallet.setCoinValue(account.confirmedAmount)

        // val outputsRes = networkProvider.getUnspentUTXOs(addressScriptHash)
        // val outputs = outputsRes.successOr { throw it.error }
        //
        // val bitcoinUnspentOut = outputs
        //     .filter {
        //         it.isConfirmed
        //     }.map { record ->
        //         BitcoinUnspentOutput(
        //             amount = record.value,
        //             outputIndex = record.txPos,
        //             transactionHash = Sha256Hash.wrap(record.txHash).reversedBytes,
        //             outputScript = addressScriptHash.encodeToByteArray(),
        //         )
        //     }
        //
        // transactionBuilder.unspentOutputs = bitcoinUnspentOut
        // outputsCount = bitcoinUnspentOut.size
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        TODO()
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val transactionSizeBytes = TEST_TRANSACTION_SIZE.toBigDecimal() // TODO

        val requiredNumberOfBlocks =
            if (amount.value == null || amount.value < HIGH_VALUE_TRANSACTION) {
                REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS
            } else {
                REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS_FOR_HIGH_VALUES
            }

        // TODO add fee amount restriction

        return networkProvider.getEstimateFee(requiredNumberOfBlocks).map {
            val minimumFeeRatePerByte = (it.feeInCoinsPer1000Bytes ?: DEFAULT_FEE_IN_COINS_PER_1000_BYTES)
                .divide(KB_DIVIDER)
            val normalFeeRatePerByte = minimumFeeRatePerByte.multiply(1.5.toBigDecimal())
                .setScale(0, RoundingMode.HALF_UP)
            val priorityFeeRatePerByte = minimumFeeRatePerByte.multiply(2.5.toBigDecimal())
                .setScale(0, RoundingMode.HALF_UP)

            val minimumFee = minimumFeeRatePerByte.multiply(transactionSizeBytes)
                .movePointLeft(Blockchain.Nexa.decimals())
            val normalFee = normalFeeRatePerByte.multiply(transactionSizeBytes)
                .movePointLeft(Blockchain.Nexa.decimals())
            val priorityFee = priorityFeeRatePerByte.multiply(transactionSizeBytes)
                .movePointLeft(Blockchain.Nexa.decimals())

            TransactionFee.Choosable(
                minimum = Fee.Common(Amount(minimumFee, Blockchain.Nexa)),
                normal = Fee.Common(Amount(normalFee, Blockchain.Nexa)),
                priority = Fee.Common(Amount(priorityFee, Blockchain.Nexa)),
            )
        }
    }

    companion object {
        private const val TEST_TRANSACTION_SIZE = 256 // TODO delete
        private val DEFAULT_FEE_IN_COINS_PER_1000_BYTES = 1000.toBigDecimal()
        private val KB_DIVIDER = 1000.toBigDecimal()
        private val HIGH_VALUE_TRANSACTION = 2_000_000_000.toBigDecimal()
        private const val REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS = 6
        private const val REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS_FOR_HIGH_VALUES = 32
    }
}