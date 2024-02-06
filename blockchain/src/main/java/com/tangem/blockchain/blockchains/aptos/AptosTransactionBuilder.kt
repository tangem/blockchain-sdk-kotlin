package com.tangem.blockchain.blockchains.aptos

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.transaction.Fee
import wallet.core.jni.CoinType
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Aptos
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput

/**
 * Aptos transaction builder
 *
 * @property wallet wallet
 */
internal class AptosTransactionBuilder(private val wallet: Wallet) {

    fun buildForSign(sequenceNumber: Long, transactionData: TransactionData, expirationTimestamp: Long): ByteArray {
        val aptosFee = transactionData.fee as? Fee.Aptos ?: throw BlockchainSdkError.FailedToBuildTx
        val input = createSigningInput(sequenceNumber, transactionData, aptosFee, expirationTimestamp).toByteArray()

        val preImageHashes = TransactionCompiler.preImageHashes(CoinType.APTOS, input)
        val output = PreSigningOutput.parseFrom(preImageHashes)

        if (output.error != Common.SigningError.OK) {
            throw BlockchainSdkError.CustomError("Error while parse preImageHashes")
        }

        return output.data.toByteArray()
    }

    fun buildForSend(
        sequenceNumber: Long,
        transactionData: TransactionData,
        expirationTimestamp: Long,
        signature: ByteArray,
    ): String {
        val aptosFee = transactionData.fee as? Fee.Aptos ?: throw BlockchainSdkError.FailedToBuildTx
        val input = createSigningInput(sequenceNumber, transactionData, aptosFee, expirationTimestamp).toByteArray()

        val publicKeys = DataVector()
        publicKeys.add(wallet.publicKey.blockchainKey)

        val signatures = DataVector()
        signatures.add(signature)

        val compileWithSignatures = TransactionCompiler.compileWithSignatures(
            CoinType.APTOS,
            input,
            signatures,
            publicKeys,
        )

        val output = Aptos.SigningOutput.parseFrom(compileWithSignatures)

        if (output.error != Common.SigningError.OK) {
            error("Something went wrong")
        }

        return output.json
    }

    private fun createSigningInput(
        sequenceNumber: Long,
        transactionData: TransactionData,
        fee: Fee.Aptos,
        expirationTimestamp: Long,
    ): Aptos.SigningInput {
        return Aptos.SigningInput.newBuilder()
            .setChainId(1)
            .setExpirationTimestampSecs(expirationTimestamp)
            .setGasUnitPrice(fee.gasUnitPrice)
            .setMaxGasAmount(fee.amount.longValue ?: 0L)
            .setSender(wallet.address)
            .setSequenceNumber(sequenceNumber)
            .setTransfer(
                Aptos.TransferMessage.newBuilder()
                    .setAmount(transactionData.amount.longValue ?: 0L)
                    .setTo(transactionData.destinationAddress),
            )
            .build()
    }
}
