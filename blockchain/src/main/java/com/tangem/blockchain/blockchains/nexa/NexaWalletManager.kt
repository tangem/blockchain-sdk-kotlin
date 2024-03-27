package com.tangem.blockchain.blockchains.nexa

import com.tangem.blockchain.blockchains.nexa.cashaddr.NexaAddressType
import com.tangem.blockchain.blockchains.nexa.models.NexaTxInputNative
import com.tangem.blockchain.blockchains.nexa.models.NexaUnspentOutput
import com.tangem.blockchain.blockchains.nexa.models.sign
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.extensions.toSimpleResult
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal
import java.math.RoundingMode

class NexaWalletManager(
    wallet: Wallet,
    private val networkProvider: ElectrumNetworkProvider,
    private val transactionBuilder: NexaTransactionBuilder,
) : WalletManager(wallet) {

    val publicKey = byteArrayOf(0x02) + wallet.publicKey.blockchainKey

    override val currentHost: String
        get() = networkProvider.baseUrl

    override val dustValue: BigDecimal =
        NexaTransactionBuilder.DUST_SATOSHI_AMOUNT
            .toBigDecimal()
            .movePointLeft(Blockchain.Nexa.decimals())

    private val addressScriptHash: String by lazy {
        NexaAddressService.getScriptHash(publicKey)
    }

    override suspend fun updateInternal() {
        val accountRes = networkProvider.getAccount(addressScriptHash)

        val account = accountRes.successOr { throw it.error }

        wallet.setCoinValue(account.confirmedAmount)

        val outputsRes = networkProvider.getUnspentUTXOs(addressScriptHash)
        val outputs = outputsRes.successOr { throw it.error }

        val bitcoinUnspentOut = outputs
            .filter {
                it.isConfirmed
            }.map { record ->
                NexaUnspentOutput(
                    amountSatoshi = record.value.movePointRight(Blockchain.Nexa.decimals()).longValueExact(),
                    outputIndex = record.txPos,
                    transactionHash = record.txHash.hexToBytes(),
                    outpointHash = record.outpointHash.hexToBytes(),
                    // for now the only supported source address type is TEMPLATE
                    addressType = NexaAddressType.TEMPLATE
                )
            }

        transactionBuilder.unspentOutputs = bitcoinUnspentOut
        outputsCount = bitcoinUnspentOut.size
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val blockTip = networkProvider.getBlockTip().successOr { return SimpleResult.fromTangemSdkError(it.error) }
        transactionBuilder.currentBlockTipHeight = blockTip.height

        val transactionForSign = transactionBuilder.buildToSign(transactionData)
            .successOr { return SimpleResult.fromTangemSdkError(it.error) }

        val inputHashes = transactionForSign.inputs.map {
            val hash = it.hashToSign as? NexaTxInputNative.SignHash.ReadyForSign ?: error("Impossible")
            hash.hash
        }

        val key = object : Wallet.PublicKey(
            seedKey = wallet.publicKey.seedKey,
            derivationType = wallet.publicKey.derivationType
        ) {
            override val blockchainKey: ByteArray = byteArrayOf(0x02) + wallet.publicKey.blockchainKey
        }

        val schnorrInputSignatures = signer.sign(inputHashes, key)
            .successOr { return SimpleResult.fromTangemSdkError(it.error) }

        val signedTransaction = transactionForSign.sign(schnorrInputSignatures, key.blockchainKey)

        val transactionHash = transactionBuilder.buildToSend(signedTransaction)

        println("ASDASD transactionHash =" + transactionHash.toHexString())

        return networkProvider.sendTransaction(
            rawTransactionHash = transactionHash.toHexString()
        ).toSimpleResult()

        //TODO add to recent transaction
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        if (transactionBuilder.currentBlockTipHeight == NexaTransactionBuilder.MAX_LOCK_TIME_BLOCK_HEIGHT) {
            val blockTip = networkProvider.getBlockTip().successOr { return Result.fromTangemSdkError(it.error) }
            transactionBuilder.currentBlockTipHeight = blockTip.height
        }

        val requiredNumberOfBlocks =
            if (amount.value == null || amount.value < HIGH_VALUE_TRANSACTION) {
                REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS
            } else {
                REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS_FOR_HIGH_VALUES
            }

        val estimateFee = networkProvider.getEstimateFee(requiredNumberOfBlocks).let {
            when (it) {
                is Result.Failure -> return it
                is Result.Success -> it.data
            }
        }

        val minimumFeeRatePerByte = (estimateFee.feeInCoinsPer1000Bytes ?: DEFAULT_FEE_IN_COINS_PER_1000_BYTES)
            .divide(KB_DIVIDER)
        val normalFeeRatePerByte = minimumFeeRatePerByte.multiply(1.5.toBigDecimal())
            .setScale(0, RoundingMode.HALF_UP)
        val priorityFeeRatePerByte = minimumFeeRatePerByte.multiply(2.5.toBigDecimal())
            .setScale(0, RoundingMode.HALF_UP)

        val avgSize = AVG_INPUT_SIZE + AVG_OUTPUT_SIZE * 2 + TX_OVERHEAD_SIZE
        val avgFee = minimumFeeRatePerByte.multiply(avgSize.toBigDecimal())
            .movePointLeft(Blockchain.Nexa.decimals())

        val newAmount = amount.copy(value = amount.value!! - avgFee)

        val transactionSizeBytes = transactionBuilder.getEstimateSize(
            TransactionData(
                amount = newAmount,
                fee = Fee.Common(Amount(newAmount, avgFee)),
                sourceAddress = wallet.address,
                destinationAddress = destination,
            )
        ).let {
            when (it) {
                is Result.Failure -> return it
                is Result.Success -> it.data
            }
        }.toBigDecimal()

        // val transactionSizeBytes = TEST_TRANSACTION_SIZE.toBigDecimal()

        // TODO add fee amount restriction

        val minimumFee = minimumFeeRatePerByte.multiply(transactionSizeBytes)
            .movePointLeft(Blockchain.Nexa.decimals())
        val normalFee = normalFeeRatePerByte.multiply(transactionSizeBytes)
            .movePointLeft(Blockchain.Nexa.decimals())
        val priorityFee = priorityFeeRatePerByte.multiply(transactionSizeBytes)
            .movePointLeft(Blockchain.Nexa.decimals())

        return Result.Success(
            TransactionFee.Choosable(
                minimum = Fee.Common(Amount(minimumFee, Blockchain.Nexa)),
                normal = Fee.Common(Amount(normalFee, Blockchain.Nexa)),
                priority = Fee.Common(Amount(priorityFee, Blockchain.Nexa)),
            )
        )
    }

    companion object {
        const val AVG_INPUT_SIZE: Long = 147
        const val AVG_OUTPUT_SIZE: Long = 34
        const val TX_OVERHEAD_SIZE: Long = 16

        private const val TEST_TRANSACTION_SIZE = 256 // TODO delete
        private val DEFAULT_FEE_IN_COINS_PER_1000_BYTES = 1000.toBigDecimal()
        private val KB_DIVIDER = 1000.toBigDecimal()
        private val HIGH_VALUE_TRANSACTION = 2_000_000_000.toBigDecimal()
        private const val REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS = 6
        private const val REQUIRED_NUMBER_OF_CONFIRMATION_BLOCKS_FOR_HIGH_VALUES = 32
    }
}