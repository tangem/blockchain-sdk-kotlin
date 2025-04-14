package com.tangem.blockchain.blockchains.algorand

import android.util.Log
import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.algorand.models.AlgorandTransactionBuildParams
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.trustWalletCoinType
import wallet.core.jni.Base64
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Algorand
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput

internal class AlgorandTransactionBuilder(blockchain: Blockchain, private val publicKey: ByteArray) {

    private val coinType = blockchain.trustWalletCoinType

    fun buildForSign(transactionData: TransactionData.Uncompiled, params: AlgorandTransactionBuildParams): ByteArray {
        val input = buildInput(transactionData, params)
        val txInputData = input.toByteArray()

        if (txInputData.isEmpty()) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        val preImageHashes = TransactionCompiler.preImageHashes(coinType, txInputData)
        val preSigningOutput = PreSigningOutput.parseFrom(preImageHashes)

        if (preSigningOutput.error != Common.SigningError.OK || preSigningOutput.data.isEmpty) {
            Log.e(this::class.java.simpleName, "AlgorandPreSigningOutput has a error: ${preSigningOutput.errorMessage}")
            throw BlockchainSdkError.FailedToBuildTx
        }

        return preSigningOutput.data.toByteArray()
    }

    fun buildForSend(
        transactionData: TransactionData,
        params: AlgorandTransactionBuildParams,
        signature: ByteArray,
    ): ByteArray {
        transactionData.requireUncompiled()

        val input = buildInput(transactionData, params)
        val txInputData = input.toByteArray()

        if (txInputData.isEmpty()) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        val signatures = DataVector()
        signatures.add(signature)

        val publicKeys = DataVector()
        publicKeys.add(publicKey)

        val compiledTransaction = TransactionCompiler.compileWithSignatures(
            coinType,
            txInputData,
            signatures,
            publicKeys,
        )

        val output = Algorand.SigningOutput.parseFrom(compiledTransaction)
        if (output.error != Common.SigningError.OK || output.encoded.isEmpty) {
            Log.e(this::class.java.simpleName, "AlgorandPreSigningOutput has a error: ${output.errorMessage}")
            throw BlockchainSdkError.FailedToBuildTx
        }

        return output.encoded.toByteArray()
    }

    private fun buildInput(
        transactionData: TransactionData,
        params: AlgorandTransactionBuildParams,
    ): Algorand.SigningInput {
        transactionData.requireUncompiled()

        val transfer = Algorand.Transfer.newBuilder()
            .setToAddress(transactionData.destinationAddress)
            .setAmount(transactionData.amount.longValue ?: 0L)
            .build()
        val input = with(Algorand.SigningInput.newBuilder()) {
            publicKey = ByteString.copyFrom(this@AlgorandTransactionBuilder.publicKey)
            genesisId = params.genesisId
            genesisHash = ByteString.copyFrom(Base64.decode(params.genesisHash))
            note = (transactionData.extras as? AlgorandTransactionExtras)
                ?.note
                ?.let(ByteString::copyFromUtf8)
                ?: ByteString.EMPTY
            firstRound = params.firstRound
            lastRound = params.lastRound
            fee = transactionData.fee?.amount?.longValue ?: 0L
            setTransfer(transfer)
        }.build()

        return input
    }
}