package com.tangem.blockchain.blockchains.near

import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.near.network.NearAmount
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.common.extensions.guard
import wallet.core.jni.Base58
import wallet.core.jni.CoinType
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.NEAR
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput

/**
[REDACTED_AUTHOR]
 */
class NearTransactionBuilder(
    private val publicKey: Wallet.PublicKey,
) {

    private val coinType = CoinType.NEAR

    // https://github.com/trustwallet/wallet-core/blob/master/android/app/src/androidTest/java/com/trustwallet/core/app/blockchains/near/TestNEARSigner.kt
    fun buildForSign(transactionData: TransactionData.Uncompiled, nonce: Long, blockHash: String): ByteArray {
        val input = createSigningInput(transactionData, nonce, blockHash)
        val txInputData = input.toByteArray()

        val preImageHashes = TransactionCompiler.preImageHashes(coinType, txInputData)
        val preSigningOutput = PreSigningOutput.parseFrom(preImageHashes)

        if (preSigningOutput.error != Common.SigningError.OK) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        return preSigningOutput.dataHash.toByteArray()
    }

    fun buildForSend(
        transactionData: TransactionData,
        signature: ByteArray,
        nonce: Long,
        blockHash: String,
    ): ByteArray {
        val input = createSigningInput(transactionData, nonce, blockHash)
        val txInputData = input.toByteArray()

        val signatures = DataVector()
        signatures.add(signature)

        val publicKeys = DataVector()
        publicKeys.add(publicKey.blockchainKey)

        val compileWithSignatures = TransactionCompiler.compileWithSignatures(
            coinType,
            txInputData,
            signatures,
            publicKeys,
        )

        val output = NEAR.SigningOutput.parseFrom(compileWithSignatures)
        if (output.error != Common.SigningError.OK) {
            error("something went wrong")
        }

        return output.signedTransaction.toByteArray()
    }

    private fun createSigningInput(
        transactionData: TransactionData,
        nonce: Long,
        blockHash: String,
    ): NEAR.SigningInput {
        transactionData.requireUncompiled()

        val sendAmountValue = transactionData.amount.value.guard {
            throw BlockchainSdkError.FailedToBuildTx
        }
        val transfer = NEAR.Transfer.newBuilder()
            .setDeposit(ByteString.copyFrom(NearAmount(sendAmountValue).toLittleEndian()))
            .build()
        val actionBuilder = NEAR.Action.newBuilder()
            .setTransfer(transfer)

        return createSigningInputWithAction(transactionData, nonce, blockHash, actionBuilder.build())
            .build()
    }

    private fun createSigningInputWithAction(
        transactionData: TransactionData,
        nonce: Long,
        blockHash: String,
        action: NEAR.Action,
    ): NEAR.SigningInput.Builder {
        transactionData.requireUncompiled()

        return NEAR.SigningInput.newBuilder()
            .setSignerId(transactionData.sourceAddress)
            .setNonce(nonce)
            .setReceiverId(transactionData.destinationAddress)
            .addActions(action)
            .setBlockHash(ByteString.copyFrom(Base58.decodeNoCheck(blockHash)))
            .setPublicKey(ByteString.copyFrom(publicKey.blockchainKey))
    }
}