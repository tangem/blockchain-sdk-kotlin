package com.tangem.blockchain.blockchains.filecoin

import com.google.protobuf.ByteString
import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinSignedTransactionBody
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.network.moshi
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toDecompressedPublicKey
import wallet.core.jni.CoinType
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.Filecoin
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput

/**
 * Filecoin transaction builder
 *
 * @property wallet wallet
 *
[REDACTED_AUTHOR]
 */
internal class FilecoinTransactionBuilder(private val wallet: Wallet) {

    fun buildForSign(nonce: Long, transactionData: TransactionData): ByteArray {
        transactionData.requireUncompiled()

        val fee = transactionData.fee as? Fee.Filecoin ?: throw BlockchainSdkError.FailedToBuildTx
        val input = createSigningInput(nonce, transactionData, fee).toByteArray()

        val preImageHashes = TransactionCompiler.preImageHashes(CoinType.FILECOIN, input)
        val output = PreSigningOutput.parseFrom(preImageHashes)

        if (output.error != Common.SigningError.OK) {
            throw BlockchainSdkError.CustomError("Error while parse preImageHashes")
        }

        return output.dataHash.toByteArray()
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun buildForSend(
        nonce: Long,
        transactionData: TransactionData,
        signature: ByteArray,
    ): FilecoinSignedTransactionBody {
        transactionData.requireUncompiled()

        val fee = transactionData.fee as? Fee.Filecoin ?: throw BlockchainSdkError.FailedToBuildTx
        val input = createSigningInput(nonce, transactionData, fee).toByteArray()

        val publicKeys = DataVector()
        publicKeys.add(wallet.publicKey.blockchainKey.toDecompressedPublicKey())

        val signatures = DataVector()
        signatures.add(signature)

        val compileWithSignatures = TransactionCompiler.compileWithSignatures(
            CoinType.FILECOIN,
            input,
            signatures,
            publicKeys,
        )

        val output = Filecoin.SigningOutput.parseFrom(compileWithSignatures)

        if (output.error != Common.SigningError.OK) {
            throw BlockchainSdkError.CustomError("Error while parse compileWithSignatures")
        }

        val filecoinSignedTransactionBody = moshi.adapter<FilecoinSignedTransactionBody>().fromJson(output.json)
            ?: throw BlockchainSdkError.FailedToBuildTx

        // If it is new account without outgoing transaction (nonce = 1)
        // Then don't put nonce value in SignedTransactionBody
        return if (nonce != 0L) {
            filecoinSignedTransactionBody.copy(
                transactionBody = filecoinSignedTransactionBody.transactionBody.copy(
                    nonce = nonce,
                ),
            )
        } else {
            filecoinSignedTransactionBody
        }
    }

    private fun createSigningInput(
        nonce: Long,
        transactionData: TransactionData,
        fee: Fee.Filecoin,
    ): Filecoin.SigningInput {
        transactionData.requireUncompiled()

        val value = transactionData.amount.value?.movePointRight(transactionData.amount.decimals)
            ?.toBigInteger()
            ?.toByteArray()
            ?: throw BlockchainSdkError.CustomError("Fail to parse amount")

        return Filecoin.SigningInput.newBuilder()
            .setTo(transactionData.destinationAddress)
            .setNonce(nonce)
            .setValue(ByteString.copyFrom(value))
            .setGasFeeCap(ByteString.copyFrom(fee.gasUnitPrice.toByteArray()))
            .setGasLimit(fee.gasLimit)
            .setGasPremium(ByteString.copyFrom(fee.gasPremium.toByteArray()))
            .setPublicKey(ByteString.copyFrom(wallet.publicKey.blockchainKey.toDecompressedPublicKey()))
            .build()
    }
}