package com.tangem.blockchain.blockchains.hedera

import com.hedera.hashgraph.sdk.*
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
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

    fun buildToSign(transactionData: TransactionData): Result<HederaBuiltTransaction> {
        val transferValue = transactionData.amount.value ?: return Result.Failure(
            BlockchainSdkError.NPError("transactionData.amount"),
        )
        val maxFeeValue = transactionData.fee?.amount?.value ?: return Result.Failure(
            BlockchainSdkError.NPError("transactionData.fee"),
        )
        val sourceAccountId = AccountId.fromString(transactionData.sourceAddress)
        val destinationAccountId = AccountId.fromString(transactionData.destinationAddress)
        val memo = (transactionData.extras as? HederaTransactionExtras)?.memo.orEmpty()

        val transaction = TransferTransaction()
            .addHbarTransfer(sourceAccountId, Hbar.from(transferValue.negate()))
            .addHbarTransfer(destinationAccountId, Hbar.from(transferValue))
            .setTransactionId(TransactionId.generate(sourceAccountId))
            .setMaxTransactionFee(Hbar.from(maxFeeValue))
            .setTransactionMemo(memo)
            .freezeWith(client)

        val bodiesToSign = transaction.innerSignedTransactions
            .map { it.bodyBytes.toByteArray() }
            .let { body ->
                if (publicKey.isED25519) {
                    body
                } else {
                    body.map { it.toKeccak() }
                }
            }

        val hederaBuiltTransaction = HederaBuiltTransaction(
            transferTransaction = transaction,
            signatures = bodiesToSign,
        )

        return Result.Success(hederaBuiltTransaction)
    }

    fun buildToSend(transaction: TransferTransaction, signatures: List<ByteArray>): TransferTransaction {
        val normalizedSignatures = if (publicKey.isED25519) signatures else signatures.map { CryptoUtils.normalize(it) }
        transaction.innerSignedTransactions.indices.forEach {
            transaction.sigPairLists[it].addSigPair(publicKey.toSignaturePairProtobuf(normalizedSignatures[it]))
        }
        return transaction
    }

    data class HederaTransactionExtras(val memo: String) : TransactionExtras
}