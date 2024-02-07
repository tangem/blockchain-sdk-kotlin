package com.tangem.blockchain.blockchains.hedera

import com.hedera.hashgraph.sdk.*
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.CryptoUtils

class HederaTransactionBuilder(
    curve: EllipticCurve,
    wallet: Wallet,
) {
    private val client = if (wallet.blockchain.isTestnet()) Client.forTestnet() else Client.forMainnet()
    val publicKey: PublicKey = when (curve) {
        EllipticCurve.Secp256k1 -> PublicKey.fromBytesECDSA(wallet.publicKey.blockchainKey)
        EllipticCurve.Ed25519,
        EllipticCurve.Ed25519Slip0010,
        -> PublicKey.fromBytesED25519(wallet.publicKey.blockchainKey)
        else -> error("unsupported curve $curve")
    }
    private var transaction: TransferTransaction? = null

    fun buildToSign(transactionData: TransactionData): Result<List<ByteArray>> {
        val transferValue = transactionData.amount.value!!
        val maxFeeValue = transactionData.fee!!.amount.value!!
        val sourceAccountId = AccountId.fromString(transactionData.sourceAddress)
        val destinationAccountId = AccountId.fromString(transactionData.destinationAddress)

        transaction = TransferTransaction()
            .addHbarTransfer(sourceAccountId, Hbar.from(transferValue.negate()))
            .addHbarTransfer(destinationAccountId, Hbar.from(transferValue))
            .setTransactionId(TransactionId.generate(sourceAccountId))
            .setMaxTransactionFee(Hbar.from(maxFeeValue))
            .freezeWith(client)

        val bodiesToSign = transaction!!.innerSignedTransactions.map { it.bodyBytes.toByteArray() }

        return if (publicKey.isED25519) {
            Result.Success(bodiesToSign)
        } else {
            Result.Success(bodiesToSign.map { it.toKeccak() })
        }
    }

    fun buildToSend(signatures: List<ByteArray>): TransferTransaction {
        val normalizedSignatures = if (publicKey.isED25519) signatures else signatures.map { CryptoUtils.normalize(it) }
        transaction!!.innerSignedTransactions.indices.forEach {
            transaction!!.sigPairLists[it].addSigPair(publicKey.toSignaturePairProtobuf(normalizedSignatures[it]))
        }
        return transaction!!
    }
}