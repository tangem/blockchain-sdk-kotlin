package com.tangem.blockchain.blockchains.hedera

import com.hedera.hashgraph.sdk.*
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import com.tangem.common.card.EllipticCurve
import java.math.BigDecimal

class HederaTransactionBuilder(
    curve: EllipticCurve,
    wallet: Wallet
) {
    private val client = if (wallet.blockchain.isTestnet()) Client.forTestnet() else Client.forMainnet()
    val publicKey: PublicKey = when (curve) {
        EllipticCurve.Secp256k1 -> PublicKey.fromBytesECDSA(wallet.publicKey.blockchainKey)
        EllipticCurve.Ed25519,
        EllipticCurve.Ed25519Slip0010 -> PublicKey.fromBytesED25519(wallet.publicKey.blockchainKey)
        else -> error("unsupported curve $curve")
    }
    private var transaction: TransferTransaction? = null

    fun buildToSign(transactionData: TransactionData): Result<List<ByteArray>> {
        val transferValue = transactionData.amount.value!!
        val maxFeeValue = transactionData.fee!!.amount.value!! * MAX_FEE_MULTIPLIER
        val sourceAccountId = AccountId.fromString(transactionData.sourceAddress)
        val destinationAccountId = AccountId.fromString(transactionData.destinationAddress)

        transaction = TransferTransaction()
            .addHbarTransfer(sourceAccountId, Hbar.from(transferValue.negate()))
            .addHbarTransfer(destinationAccountId, Hbar.from(transferValue))
            .setTransactionId(TransactionId.generate(sourceAccountId))
            .setMaxTransactionFee(Hbar.from(maxFeeValue))
            .freezeWith(client)

        val bodiesToSign = transaction!!.innerSignedTransactions.map { it.bodyBytes.toByteArray() }

        return Result.Success(bodiesToSign)
    }

    fun buildToSend(signatures: List<ByteArray>): TransferTransaction {
        transaction!!.innerSignedTransactions.indices.forEach {
            transaction!!.sigPairLists[it].addSigPair(publicKey.toSignaturePairProtobuf(signatures[it]))
        }
        return transaction!!
    }

    companion object {
        // Hedera fees are low, allow 10% safety margin to allow usage of not precise fee estimate
        private val MAX_FEE_MULTIPLIER = BigDecimal("1.1")
    }
}
