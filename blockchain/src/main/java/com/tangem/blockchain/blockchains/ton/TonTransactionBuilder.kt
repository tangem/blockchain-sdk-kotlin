package com.tangem.blockchain.blockchains.ton

import com.google.protobuf.ByteString
import com.tangem.blockchain.common.Amount
import com.tangem.common.KeyPair
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.CryptoUtils
import wallet.core.jni.proto.TheOpenNetwork

class TonTransactionBuilder {

    private val keyPair: KeyPair by lazy { generateKeyPair() }
    private val modeTransactionConstant: Int =
        TheOpenNetwork.SendMode.PAY_FEES_SEPARATELY_VALUE.or(TheOpenNetwork.SendMode.IGNORE_ACTION_PHASE_ERRORS_VALUE)

    fun buildForSign(
        sequenceNumber: Int,
        amount: Amount,
        destination: String,
        extras: TonTransactionExtras? = null,
    ): TheOpenNetwork.SigningInput {
        return input(sequenceNumber, amount, destination, extras?.memo.orEmpty())
    }

    fun buildForSend(output: TheOpenNetwork.SigningOutput): String {
        return output.encoded
    }

    private fun input(
        sequenceNumber: Int,
        amount: Amount,
        destination: String,
        comment: String,
    ): TheOpenNetwork.SigningInput {
        val transfer = TheOpenNetwork.Transfer
            .newBuilder()
            .setWalletVersion(TheOpenNetwork.WalletVersion.WALLET_V4_R2)
            .setDest(destination)
            .setAmount(amount.longValue ?: 0L)
            .setSequenceNumber(sequenceNumber)
            .setMode(modeTransactionConstant)
            .setBounceable(false)
            .setComment(comment)

        return TheOpenNetwork.SigningInput
            .newBuilder()
            .setTransfer(transfer)
            .setPrivateKey(ByteString.copyFrom(keyPair.privateKey))
            .build()
    }

    private fun generateKeyPair(): KeyPair {
        val privateKey = CryptoUtils.generateRandomBytes(32)
        val publicKey = CryptoUtils.generatePublicKey(privateKey, EllipticCurve.Ed25519)
        return KeyPair(publicKey, privateKey)
    }
}
