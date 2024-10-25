package com.tangem.blockchain.blockchains.casper

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.casper.CasperTransactionBuilder.Companion.DEFAULT_GAS_PRICE
import com.tangem.blockchain.blockchains.casper.CasperTransactionBuilder.Companion.DEFAULT_TTL_FORMATTED
import com.tangem.blockchain.blockchains.casper.network.request.CasperTransactionBody
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.extensions.hexToBytes
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class CasperTransactionBuilderTest {

    private val blockchain = Blockchain.Casper

    private val txBuilder = CasperTransactionBuilder(
        wallet = Wallet(
            blockchain = blockchain,
            addresses = setOf(Address(SOURCE_ADDRESS)),
            publicKey = Wallet.PublicKey(
                seedKey = PUBLIC_KEY.hexToBytes(),
                derivationType = null,
            ),
            tokens = setOf(),
        ),
    )

    @Test
    fun buildForSign() {
        val expected = "951f30645f15e5955750d7aa3b50cadd8ca4044f46aa49cfe389d90825f8122f".hexToBytes()

        val actual = buildUnsigned().hash.hexToBytes()

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun buildForSend() {
        val approval = CasperTransactionBody.Approval(
            signer = SOURCE_ADDRESS,
            signature = SIGNATURE,
        )

        val session = CasperTransactionBody.Session(
            transfer = CasperTransactionBody.Session.Transfer(
                args = listOf(
                    listOf(
                        "amount",
                        CasperTransactionBody.CLValue(
                            bytes = "0400f90295",
                            clType = CasperTransactionBody.CLType.U512,
                            parsed = BigInteger("2500000000"),
                        ),
                    ),
                    listOf(
                        "target",
                        CasperTransactionBody.CLValue(
                            bytes = DESTINATION_ADDRESS,
                            clType = CasperTransactionBody.CLType.PublicKey,
                            parsed = DESTINATION_ADDRESS,
                        ),
                    ),
                    listOf(
                        "id",
                        CasperTransactionBody.CLValue(
                            bytes = "00",
                            clType = CasperTransactionBody.CLType.Option(
                                innerType = CasperTransactionBody.CLType.U64,
                            ),
                            parsed = null,
                        ),
                    ),
                ),
            ),
        )

        val payment = CasperTransactionBody.Payment(
            moduleBytesObj = CasperTransactionBody.Payment.ModuleBytes(
                moduleBytes = "",
                args = listOf(
                    listOf(
                        "amount",
                        CasperTransactionBody.CLValue(
                            bytes = "0400e1f505",
                            clType = CasperTransactionBody.CLType.U512,
                            parsed = BigInteger("100000000"),
                        ),
                    ),
                ),
            ),
        )

        val header = CasperTransactionBody.Header(
            account = SOURCE_ADDRESS,
            timestamp = TIMESTAMP,
            ttl = DEFAULT_TTL_FORMATTED,
            gasPrice = DEFAULT_GAS_PRICE,
            bodyHash = "539eb1d8af818301392e20ebe00339b11b2442c67a09879851d76a7d0ef20d73",
            chainName = "casper",
        )

        val expected = CasperTransactionBody(
            hash = "951f30645f15e5955750d7aa3b50cadd8ca4044f46aa49cfe389d90825f8122f",
            header = header,
            payment = payment,
            session = session,
            approvals = listOf(approval),
        )

        DateTimeUtils.setCurrentMillisFixed(DateTime.parse(TIMESTAMP).millis)

        val unsigned = buildUnsigned()
        val actual = txBuilder.buildForSend(
            unsignedTransactionBody = unsigned,
            signature = SIGNATURE.hexToBytes(),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun buildUnsigned(): CasperTransactionBody {
        return txBuilder.buildForSign(
            transactionData = TransactionData.Uncompiled(
                amount = CSPR_AMOUNT.copy(value = BigDecimal(2.5)),
                fee = Fee.Common(
                    amount = CSPR_AMOUNT.copy(
                        value = BigDecimal(0.1),
                    ),
                ),
                sourceAddress = SOURCE_ADDRESS,
                destinationAddress = DESTINATION_ADDRESS,
                status = TransactionStatus.Unconfirmed,
                extras = CasperTransactionExtras(null),
            ),
        )
    }

    private companion object {
        const val PUBLIC_KEY = "03ae9bdc765678be0ef74c3845f1f506fa8dbbef7a57aaa39a40daafc13dc9ac60"
        const val SIGNATURE = "020d735191dbc378a30d9c122384bf77169d165d0123ce16c31cf3d86cb213aa1b26842d9e204f0c2c5f6719f1371fd9710d01b766bd724a099c45305fae776185"
        const val SOURCE_ADDRESS = "0203ae9bdc765678be0ef74c3845f1f506fa8dbbef7a57aaa39a40daafc13dc9ac60"
        const val DESTINATION_ADDRESS = "0198c07d7e72d89a681d7227a7af8a6fd5f22fe0105c8741d55a95df415454b82e"
        const val TIMESTAMP = "2024-10-12T12:04:41.031Z"

        val CSPR_AMOUNT = Amount(currencySymbol = "CSPR", value = null, decimals = 9, type = AmountType.Coin)
    }
}