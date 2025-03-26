package com.tangem.blockchain.blockchains.hedera

import android.util.Log
import com.hedera.hashgraph.sdk.*
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.blockchains.hedera.models.TokenAssociation
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.CryptoUtils

internal class HederaTransactionBuilder(
    curve: EllipticCurve,
    wallet: Wallet,
) {
    private val client = if (wallet.blockchain.isTestnet()) Client.forTestnet() else Client.forMainnet()
    private val publicKey: PublicKey = when (curve) {
        EllipticCurve.Secp256k1 -> PublicKey.fromBytesECDSA(wallet.publicKey.blockchainKey)
        EllipticCurve.Ed25519,
        EllipticCurve.Ed25519Slip0010,
        -> PublicKey.fromBytesED25519(wallet.publicKey.blockchainKey)
        else -> error("unsupported curve $curve")
    }

    fun buildToSign(transactionData: TransactionData): Result<HederaBuiltTransaction<TransferTransaction>> {
        return try {
            transactionData.requireUncompiled()

            val fee = transactionData.fee as? Fee.Hedera ?: return Result.Failure(
                BlockchainSdkError.CustomError("transactionData.fee is not Fee.Hedera"),
            )

            val totalFeeValue = fee.amount.value ?: return Result.Failure(
                BlockchainSdkError.NPError("Fee amount value must not be null"),
            )
            val additionalHBARFee = fee.additionalHBARFee

            val actualFeeValue = totalFeeValue - additionalHBARFee
            val maxTransactionFee = Hbar.from(actualFeeValue)

            val sourceAccountId = AccountId.fromString(transactionData.sourceAddress)
            val destinationAccountId = AccountId.fromString(transactionData.destinationAddress)
            val memo = (transactionData.extras as? HederaTransactionExtras)?.memo.orEmpty()

            val transaction = makeTransferTransaction(
                amount = transactionData.amount,
                sourceAccountId = sourceAccountId,
                destinationAccountId = destinationAccountId,
            )
                .setTransactionId(TransactionId.generate(sourceAccountId))
                .setMaxTransactionFee(maxTransactionFee)
                .setTransactionMemo(memo)
                .freezeWith(client)

            val bodiesToSign = transaction.innerSignedTransactions
                .map { it.bodyBytes.toByteArray() }
                .correctWithPublicKey()

            val hederaBuiltTransaction = HederaBuiltTransaction(
                transaction = transaction,
                signatures = bodiesToSign,
            )

            Result.Success(hederaBuiltTransaction)
        } catch (e: BlockchainSdkError) {
            Result.Failure(e)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    fun buildTokenAssociationForSign(
        tokenAssociation: TokenAssociation,
    ): Result<HederaBuiltTransaction<TokenAssociateTransaction>> {
        return try {
            val accountId = AccountId.fromString(tokenAssociation.accountId)
            val tokenId = HederaUtils.createTokenId(tokenAssociation.contractAddress)
            val tokenAssociateTransaction = with(TokenAssociateTransaction()) {
                setAccountId(accountId)
                tokenIds = listOf(tokenId)
                transactionId = TransactionId.generate(accountId)
                freezeWith(client)
            }

            val bodiesToSign = tokenAssociateTransaction.innerSignedTransactions
                .map { it.bodyBytes.toByteArray() }
                .correctWithPublicKey()

            Result.Success(
                HederaBuiltTransaction(
                    transaction = tokenAssociateTransaction,
                    signatures = bodiesToSign,
                ),
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    fun <T : Transaction<T>> buildToSend(transaction: Transaction<T>, signatures: List<ByteArray>): Transaction<T> {
        val normalizedSignatures = if (publicKey.isED25519) signatures else signatures.map { CryptoUtils.normalize(it) }
        transaction.innerSignedTransactions.indices.forEach {
            transaction.sigPairLists[it].addSigPair(publicKey.toSignaturePairProtobuf(normalizedSignatures[it]))
        }
        return transaction
    }

    private fun makeTransferTransaction(
        amount: Amount,
        sourceAccountId: AccountId,
        destinationAccountId: AccountId,
    ): TransferTransaction {
        val transactionValue = amount.longValue ?: throw BlockchainSdkError.NPError("transactionData.amount")
        return when (val amountType = amount.type) {
            AmountType.Coin -> TransferTransaction()
                .addHbarTransfer(sourceAccountId, Hbar.fromTinybars(transactionValue.unaryMinus()))
                .addHbarTransfer(destinationAccountId, Hbar.fromTinybars(transactionValue))
            is AmountType.Token -> {
                val tokenId = HederaUtils.createTokenId(amountType.token.contractAddress)
                TransferTransaction()
                    .addTokenTransfer(tokenId, sourceAccountId, transactionValue.unaryMinus())
                    .addTokenTransfer(tokenId, destinationAccountId, transactionValue)
            }
            else -> throw BlockchainSdkError.FailedToBuildTx
        }
    }

    private fun List<ByteArray>.correctWithPublicKey(): List<ByteArray> =
        if (publicKey.isED25519) this else this.map { it.toKeccak() }
}