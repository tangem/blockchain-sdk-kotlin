package com.tangem.blockchain.blockchains.aptos

import com.tangem.blockchain.blockchains.aptos.models.AptosTransactionInfo
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.extensions.toHexString
import org.joda.time.DateTime

/**
 * Aptos transaction builder
 *
 * @property wallet wallet
 */
internal class AptosTransactionBuilder(private val wallet: Wallet) {

    /**
     * Build pseudo transaction to calculate required fee to send it
     *
     * @param sequenceNumber account sequence number
     * @param destination    destination address
     * @param amount         amount
     * @param gasUnitPrice   gas unit price
     */
    fun buildToCalculateFee(
        sequenceNumber: Long,
        destination: String,
        amount: Amount,
        gasUnitPrice: Long,
    ): AptosTransactionInfo {
        return AptosTransactionInfo(
            sequenceNumber = sequenceNumber,
            publicKey = wallet.getPublicKey(),
            sourceAddress = wallet.address,
            destinationAddress = destination,
            amount = amount.longValue ?: 0L,
            gasUnitPrice = gasUnitPrice,
            maxGasAmount = PSEUDO_TRANSACTION_MAX_GAS_AMOUNT,
            expirationTimestamp = createExpirationTimestamp(),
            hash = PSEUDO_TRANSACTION_HASH,
        )
    }

    /**
     * Build transaction to encode
     *
     * @param sequenceNumber  account sequence number
     * @param transactionData transaction data
     */
    @Throws(BlockchainSdkError.FailedToBuildTx::class)
    fun buildToEncode(sequenceNumber: Long, transactionData: TransactionData): AptosTransactionInfo {
        val aptosFee = transactionData.fee as? Fee.Aptos ?: throw BlockchainSdkError.FailedToBuildTx

        return AptosTransactionInfo(
            sequenceNumber = sequenceNumber,
            publicKey = wallet.getPublicKey(),
            sourceAddress = wallet.address,
            destinationAddress = transactionData.destinationAddress,
            amount = transactionData.amount.longValue ?: 0L,
            gasUnitPrice = aptosFee.gasUnitPrice,
            maxGasAmount = aptosFee.amount.longValue ?: 0L,
            expirationTimestamp = createExpirationTimestamp(),
        )
    }

    /**
     * Build transaction to send using encoded and signed transaction
     *
     * @param transaction encoded transaction
     * @param hash        encoded and signed transaction hash
     */
    fun buildToSend(transaction: AptosTransactionInfo, hash: ByteArray): AptosTransactionInfo {
        return transaction.copy(hash = hash.toHexStringWithPrefix())
    }

    private fun Wallet.getPublicKey(): String = publicKey.blockchainKey.toHexStringWithPrefix()

    private fun ByteArray.toHexStringWithPrefix(): String = "0x" + toHexString().lowercase()

    private fun createExpirationTimestamp(): Long = DateTime.now().plusMinutes(TRANSACTION_LIFETIME_IN_MIN).seconds()

    private fun DateTime.seconds(): Long = millis.div(other = 1000)

    private companion object {

        const val TRANSACTION_LIFETIME_IN_MIN = 5

        /** Max gas amount doesn't matter for fee calculating */
        const val PSEUDO_TRANSACTION_MAX_GAS_AMOUNT = 100_000L

        /** Transaction hash doesn't matter for fee calculating */
        const val PSEUDO_TRANSACTION_HASH = "0x000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000"
    }
}