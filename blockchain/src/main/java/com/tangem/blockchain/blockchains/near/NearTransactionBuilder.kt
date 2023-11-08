package com.tangem.blockchain.blockchains.near

import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.near.network.NearAmount
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.common.KeyPair
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.guard
import com.tangem.crypto.CryptoUtils
import wallet.core.jni.*
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.NEAR
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput

/**
 * @author Anton Zhilenkov on 03.08.2023.
 */
class NearTransactionBuilder(
    private val publicKey: Wallet.PublicKey,
) {

    private val coinType = CoinType.NEAR
    private val keyPair: KeyPair by lazy { generateKeyPair() }

    // https://github.com/trustwallet/wallet-core/blob/master/android/app/src/androidTest/java/com/trustwallet/core/app/blockchains/near/TestNEARSigner.kt
    fun buildForSign(
        transaction: TransactionData,
        nonce: Long,
        blockHash: String,
    ): ByteArray {
        val input = createSigningInput(transaction, nonce, blockHash)
        val txInputData = input.toByteArray()

        val preImageHashes = TransactionCompiler.preImageHashes(coinType, txInputData)
        val preSigningOutput = PreSigningOutput.parseFrom(preImageHashes)

        if (preSigningOutput.error != Common.SigningError.OK) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        return preSigningOutput.dataHash.toByteArray()
    }

    fun buildForSend(
        transaction: TransactionData,
        signature: ByteArray,
        nonce: Long,
        blockHash: String,
    ): ByteArray {
        val input = createSigningInput(transaction, nonce, blockHash)
        val txInputData = input.toByteArray()

        val signatures = DataVector()
        signatures.add(signature)

        val publicKeys = DataVector()
        publicKeys.add(publicKey.blockchainKey)

        val compileWithSignatures = TransactionCompiler.compileWithSignatures(
            coinType, txInputData, signatures, publicKeys
        )

        val output = NEAR.SigningOutput.parseFrom(compileWithSignatures)
        if (output.error != Common.SigningError.OK) {
            throw IllegalStateException("something went wrong")
        }

        return output.signedTransaction.toByteArray()
    }

    private fun createSigningInput(
        transaction: TransactionData,
        nonce: Long,
        blockHash: String,
    ): NEAR.SigningInput {
        val sendAmountValue = transaction.amount.value.guard {
            throw BlockchainSdkError.FailedToBuildTx
        }
        val transfer = NEAR.Transfer.newBuilder()
            .setDeposit(ByteString.copyFrom(NearAmount(sendAmountValue).toLittleEndian()))
            .build()
        val actionBuilder = NEAR.Action.newBuilder()
            .setTransfer(transfer)

        return createSigningInputWithAction(transaction, nonce, blockHash, actionBuilder.build())
            .build()
    }

    private fun createSigningInputWithAction(
        transaction: TransactionData,
        nonce: Long,
        blockHash: String,
        action: NEAR.Action,
    ): NEAR.SigningInput.Builder {
        return NEAR.SigningInput.newBuilder()
            .setSignerId(transaction.sourceAddress)
            .setNonce(nonce)
            .setReceiverId(transaction.destinationAddress)
            .addActions(action)
            .setBlockHash(ByteString.copyFrom(Base58.decodeNoCheck(blockHash)))
            .setPublicKey(ByteString.copyFrom(publicKey.blockchainKey))
    }

    private fun generateKeyPair(): KeyPair {
        val privateKey = CryptoUtils.generateRandomBytes(32)
        val publicKey = CryptoUtils.generatePublicKey(privateKey, EllipticCurve.Ed25519)
        return KeyPair(publicKey, privateKey)
    }
}
