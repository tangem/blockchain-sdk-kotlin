package com.tangem.blockchain.blockchains.ton

import com.google.protobuf.ByteString
import com.tangem.blockchain.common.*
import wallet.core.jni.CoinType
import wallet.core.jni.DataVector
import wallet.core.jni.TransactionCompiler
import wallet.core.jni.proto.Common
import wallet.core.jni.proto.TheOpenNetwork
import wallet.core.jni.proto.TransactionCompiler.PreSigningOutput

internal class TonTransactionBuilder(
    private val publicKey: Wallet.PublicKey,
    private val walletAddress: String,
) {

    private val coinType = CoinType.TON
    private var jettonWalletAddresses: Map<Token, String> = emptyMap()
    private val modeTransactionConstant: Int =
        TheOpenNetwork.SendMode.PAY_FEES_SEPARATELY_VALUE.or(TheOpenNetwork.SendMode.IGNORE_ACTION_PHASE_ERRORS_VALUE)

    fun updateJettonAdresses(fetchedJettonWalletAddresses: Map<Token, String>) {
        jettonWalletAddresses = fetchedJettonWalletAddresses
    }

    fun buildForSign(
        sequenceNumber: Int,
        amount: Amount,
        destination: String,
        expireAt: Int,
        extras: TonTransactionExtras? = null,
    ): ByteArray {
        val input = buildInput(
            sequenceNumber = sequenceNumber,
            amount = amount,
            destination = destination,
            comment = extras?.memo.orEmpty(),
            expireAt = expireAt,
        )
        val preImageHashes = TransactionCompiler.preImageHashes(coinType, input.toByteArray())
        val preSigningOutput = PreSigningOutput.parseFrom(preImageHashes)

        if (preSigningOutput.error != Common.SigningError.OK) {
            throw BlockchainSdkError.FailedToBuildTx
        }

        return preSigningOutput.data.toByteArray()
    }

    fun buildForSend(
        signature: ByteArray,
        sequenceNumber: Int,
        amount: Amount,
        destination: String,
        expireAt: Int,
        extras: TonTransactionExtras? = null,
    ): String {
        val input = buildInput(
            sequenceNumber = sequenceNumber,
            amount = amount,
            destination = destination,
            comment = extras?.memo.orEmpty(),
            expireAt = expireAt,
        )
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

        val output = TheOpenNetwork.SigningOutput.parseFrom(compileWithSignatures)
        if (output.error != Common.SigningError.OK) {
            error("something went wrong")
        }

        return output.encoded
    }

    private fun buildInput(
        sequenceNumber: Int,
        amount: Amount,
        destination: String,
        comment: String,
        expireAt: Int,
    ): TheOpenNetwork.SigningInput {
        val transfer = when (amount.type) {
            is AmountType.Coin -> makeTransfer(amount = amount, destination = destination, comment = comment)
            is AmountType.Token -> makeJettonTransfer(amount = amount, destination = destination, comment = comment)
            else -> throw BlockchainSdkError.FailedToBuildTx
        }

        return TheOpenNetwork.SigningInput
            .newBuilder()
            .addMessages(transfer)
            .setWalletVersion(TheOpenNetwork.WalletVersion.WALLET_V4_R2)
            .setSequenceNumber(sequenceNumber)
            .setExpireAt(expireAt)
            .setPublicKey(ByteString.copyFrom(publicKey.blockchainKey))
            .build()
    }

    private fun makeTransfer(
        amount: Amount,
        destination: String,
        comment: String,
        jettonTransfer: TheOpenNetwork.JettonTransfer? = null,
    ): TheOpenNetwork.Transfer {
        return TheOpenNetwork.Transfer.newBuilder()
            .setDest(destination)
            .setAmount(amount.longValue)
            .setMode(modeTransactionConstant)
            .setBounceable(false)
            .setComment(comment)
            .also { transfer -> if (jettonTransfer != null) transfer.setJettonTransfer(jettonTransfer) }
            .build()
    }

    private fun makeJettonTransfer(amount: Amount, destination: String, comment: String): TheOpenNetwork.Transfer {
        val token = (amount.type as? AmountType.Token)?.token ?: throw BlockchainSdkError.FailedToBuildTx
        val jettonWalletAddress = jettonWalletAddresses[token] ?: throw BlockchainSdkError.FailedToBuildTx

        val amountValue = requireNotNull(amount.value) { "Amount value must not be null" }
        val jettonAmount = amountValue
            .movePointRight(amount.decimals)
            .toBigInteger()
            .toByteArray()
            .let(ByteString::copyFrom)
        val jettonTransfer = TheOpenNetwork.JettonTransfer.newBuilder()
            .setJettonAmount(jettonAmount)
            .setToOwner(destination)
            .setResponseAddress(walletAddress)
            .setForwardAmount(1L) // needs some amount to send "jetton transfer notification", use minimum
            .build()

        return makeTransfer(
            amount = Amount(JETTON_TRANSFER_PROCESSING_FEE, Blockchain.TON),
            destination = jettonWalletAddress,
            comment = comment,
            jettonTransfer = jettonTransfer,
        )
    }

    internal companion object {
        /* used to cover token transfer fees, commonly used value after TON fee reduction, actual costs now are ~10
         * times less, excess is returned
         */
        val JETTON_TRANSFER_PROCESSING_FEE = "0.05".toBigDecimal()
    }
}