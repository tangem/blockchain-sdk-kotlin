package com.tangem.blockchain.blockchains.ton

import com.google.protobuf.ByteString
import com.tangem.blockchain.common.*
import com.tangem.common.KeyPair
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.CryptoUtils
import wallet.core.jni.proto.TheOpenNetwork
import com.tangem.blockchain.extensions.Result

internal class TonTransactionBuilder(private val walletAddress: String) {

    private val keyPair: KeyPair by lazy { generateKeyPair() }
    private val modeTransactionConstant: Int =
        TheOpenNetwork.SendMode.PAY_FEES_SEPARATELY_VALUE.or(TheOpenNetwork.SendMode.IGNORE_ACTION_PHASE_ERRORS_VALUE)

    private var jettonWalletAddresses: Map<Token, String> = emptyMap()

    fun updateJettonAdresses(fetchedJettonWalletAddresses: Map<Token, String>) {
        jettonWalletAddresses = fetchedJettonWalletAddresses
    }

    fun buildForSign(
        sequenceNumber: Int,
        amount: Amount,
        destination: String,
        extras: TonTransactionExtras? = null,
    ): Result<TheOpenNetwork.SigningInput> {
        return when (amount.type) {
            is AmountType.Coin -> buildCoinTransferInput(sequenceNumber, amount, destination, extras?.memo.orEmpty())
            is AmountType.Token -> buildTokenTransferInput(sequenceNumber, amount, destination, extras?.memo.orEmpty())
            else -> return Result.Failure(BlockchainSdkError.FailedToBuildTx)
        }
    }

    fun buildForSend(output: TheOpenNetwork.SigningOutput): String {
        return output.encoded
    }

    private fun buildCoinTransferInput(
        sequenceNumber: Int,
        amount: Amount,
        destination: String,
        comment: String,
    ): Result<TheOpenNetwork.SigningInput> {
        val transfer = buildTransfer(sequenceNumber, amount, destination, comment)

        return Result.Success(
            TheOpenNetwork.SigningInput
                .newBuilder()
                .setTransfer(transfer)
                .setPrivateKey(ByteString.copyFrom(keyPair.privateKey))
                .build(),
        )
    }

    private fun buildTokenTransferInput(
        sequenceNumber: Int,
        amount: Amount,
        destination: String,
        comment: String,
    ): Result<TheOpenNetwork.SigningInput> {
        val token = (amount.type as AmountType.Token).token
        val jettonWalletAddress = jettonWalletAddresses[token]
            ?: return Result.Failure(BlockchainSdkError.FailedToBuildTx)
        val transfer = buildTransfer(
            sequenceNumber = sequenceNumber,
            amount = Amount(JETTON_TRANSFER_PROCESSING_FEE, Blockchain.TON),
            destination = jettonWalletAddress,
            comment = comment,
            bounceable = true,
        )

        val jettonTransfer = TheOpenNetwork.JettonTransfer.newBuilder()
            .setTransfer(transfer)
            .setJettonAmount(amount.longValueOrZero)
            .setToOwner(destination)
            .setResponseAddress(walletAddress)
            .setForwardAmount(1) // some amount needed to send "jetton transfer notification", use minimum
            .build()

        return Result.Success(
            TheOpenNetwork.SigningInput
                .newBuilder()
                .setJettonTransfer(jettonTransfer)
                .setPrivateKey(ByteString.copyFrom(keyPair.privateKey))
                .build(),
        )
    }

    private fun buildTransfer(
        sequenceNumber: Int,
        amount: Amount,
        destination: String,
        comment: String,
        bounceable: Boolean = false,
    ): TheOpenNetwork.Transfer.Builder? {
        return TheOpenNetwork.Transfer.newBuilder()
            .setWalletVersion(TheOpenNetwork.WalletVersion.WALLET_V4_R2)
            .setDest(destination)
            .setAmount(amount.longValueOrZero)
            .setSequenceNumber(sequenceNumber)
            .setMode(modeTransactionConstant)
            .setBounceable(bounceable)
            .setComment(comment)
    }

    @Suppress("MagicNumber")
    private fun generateKeyPair(): KeyPair {
        val privateKey = CryptoUtils.generateRandomBytes(32)
        /* todo use Ed25519Slip0010 or Ed25519 depends on wallet manager
         * [REDACTED_JIRA]
         */
        val publicKey = CryptoUtils.generatePublicKey(privateKey, EllipticCurve.Ed25519)
        return KeyPair(publicKey, privateKey)
    }

    internal companion object {
        /* used to cover token transfer fees, commonly used value after TON fee reduction, actual costs now are ~10
         * times less, excess is returned
         */
        val JETTON_TRANSFER_PROCESSING_FEE = "0.05".toBigDecimal()
    }
}