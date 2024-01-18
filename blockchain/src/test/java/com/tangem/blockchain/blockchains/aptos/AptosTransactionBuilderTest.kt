package com.tangem.blockchain.blockchains.aptos

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.aptos.models.AptosTransactionInfo
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.CryptoUtils
import org.joda.time.DateTime
import org.junit.Test
import java.math.BigDecimal

internal class AptosTransactionBuilderTest {

    private val txBuilder = AptosTransactionBuilder(
        wallet = Wallet(
            blockchain = Blockchain.Aptos,
            addresses = setOf(Address(SOURCE_ADDRESS)),
            publicKey = Wallet.PublicKey(
                seedKey = PUBLIC_KEY.hexToBytes(),
                derivationType = null,
            ),
            tokens = setOf(),
        ),
    )

    @Test
    fun buildToCalculateFee() {
        val expected = AptosTransactionInfo(
            sequenceNumber = 1,
            publicKey = PUBLIC_KEY,
            sourceAddress = SOURCE_ADDRESS,
            destinationAddress = DESTINATION_ADDRESS,
            amount = 100_000_000,
            contractAddress = null,
            gasUnitPrice = 100,
            maxGasAmount = AptosTransactionBuilder.PSEUDO_TRANSACTION_MAX_GAS_AMOUNT,
            expirationTimestamp = createExpirationTimestamp(),
            hash = AptosTransactionBuilder.PSEUDO_TRANSACTION_HASH,
        )

        val actual = txBuilder.buildToCalculateFee(
            sequenceNumber = 1,
            destination = DESTINATION_ADDRESS,
            amount = APT_AMOUNT.copy(value = BigDecimal(1)),
            gasUnitPrice = 100,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun buildToEncode() {
        val expected = AptosTransactionInfo(
            sequenceNumber = 1,
            publicKey = PUBLIC_KEY,
            sourceAddress = SOURCE_ADDRESS,
            destinationAddress = DESTINATION_ADDRESS,
            amount = 1_000_000,
            contractAddress = null,
            gasUnitPrice = 100,
            maxGasAmount = 9,
            expirationTimestamp = createExpirationTimestamp(),
            hash = null,
        )

        val actual = txBuilder.buildToEncode(
            sequenceNumber = 1,
            transactionData = TransactionData(
                amount = APT_AMOUNT.copy(value = BigDecimal(0.01)),
                fee = Fee.Aptos(
                    amount = APT_AMOUNT.copy(value = BigDecimal(9).movePointLeft(8)),
                    gasUnitPrice = 100,
                ),
                sourceAddress = SOURCE_ADDRESS,
                destinationAddress = DESTINATION_ADDRESS,
                status = TransactionStatus.Unconfirmed,
                date = null,
                hash = null,
                extras = null,
                contractAddress = null,
            ),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun buildToSend() {
        val hash = CryptoUtils.generateRandomBytes(64)
        val expected = AptosTransactionInfo(
            sequenceNumber = 1,
            publicKey = PUBLIC_KEY,
            sourceAddress = SOURCE_ADDRESS,
            destinationAddress = DESTINATION_ADDRESS,
            amount = 1_000_000,
            contractAddress = null,
            gasUnitPrice = 100,
            maxGasAmount = 9,
            expirationTimestamp = createExpirationTimestamp(),
            hash = "0x" + hash.toHexString().lowercase(),
        )

        val actual = txBuilder.buildToSend(transaction = expected, hash = hash)

        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun createExpirationTimestamp(): Long {
        return DateTime.now().plusMinutes(AptosTransactionBuilder.TRANSACTION_LIFETIME_IN_MIN).seconds()
    }

    private fun DateTime.seconds(): Long = millis.div(other = 1000)

    private companion object {
        const val PUBLIC_KEY = "0x62e7a6a486553b56a53e89dfae3f780693e537e5b0a7ed33290780e581ca8369"
        const val SOURCE_ADDRESS = "0x1869b853768f0ba935d67f837a66b172dd39a60ca2315f8d4e0e669bbd35cf25"
        const val DESTINATION_ADDRESS = "0x07968dab936c1bad187c60ce4082f307d030d780e91e694ae03aef16aba73f30"

        val APT_AMOUNT = Amount(currencySymbol = "APT", value = null, decimals = 8, type = AmountType.Coin)
    }
}