package com.tangem.blockchain.blockchains.aptos

import com.google.common.truth.Truth
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import java.math.BigDecimal

internal class AptosTransactionBuilderTest {

    init {
        System.loadLibrary("TrustWalletCore")
    }

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
    fun buildForSign() {
        val expected = (
            "B5E97DB07FA0BD0E5598AA3643A9BC6F6693BDDC1A9FEC9E674A461EAA00B1931869B853768F0BA935D67F837A66B172DD39A60" +
                "CA2315F8D4E0E669BBD35CF2501000000000000000200000000000000000000000000000000000000000000000000000000" +
                "000000010D6170746F735F6163636F756E74087472616E7366657200022007968DAB936C1BAD187C60CE4082F307D030D78" +
                "0E91E694AE03AEF16ABA73F300800E1F505000000000900000000000000640000000000000031F8C0650000000001"
            )
            .hexToBytes()

        val actual = txBuilder.buildForSign(
            sequenceNumber = 1,
            transactionData = TransactionData(
                amount = APT_AMOUNT.copy(value = BigDecimal(1)),
                fee = Fee.Aptos(
                    amount = APT_AMOUNT.copy(value = BigDecimal(9).movePointLeft(8)),
                    gasUnitPrice = 100,
                ),
                sourceAddress = SOURCE_ADDRESS,
                destinationAddress = DESTINATION_ADDRESS,
                status = TransactionStatus.Unconfirmed,
            ),
            expirationTimestamp = 1707145265,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun buildForSend() {
        val expected = "{" +
            "\"expiration_timestamp_secs\":\"1707145265\"," +
            "\"gas_unit_price\":\"100\"," +
            "\"max_gas_amount\":\"9\"," +
            "\"payload\":" +
            "{" +
            "\"arguments\":[\"0x7968dab936c1bad187c60ce4082f307d030d780e91e694ae03aef16aba73f30\",\"100000000\"]," +
            "\"function\":\"0x1::aptos_account::transfer\"," +
            "\"type\":\"entry_function_payload\"," +
            "\"type_arguments\":[]" +
            "}," +
            "\"sender\":\"0x1869b853768f0ba935d67f837a66b172dd39a60ca2315f8d4e0e669bbd35cf25\"," +
            "\"sequence_number\":\"1\"," +
            "\"signature\":" +
            "{" +
            "\"public_key\":\"0x62e7a6a486553b56a53e89dfae3f780693e537e5b0a7ed33290780e581ca8369\"," +
            "\"signature\":\"0xb5e97db07fa0bd0e5598aa3643a9bc6f6693bddc1a9fec9e674a461eaa00b1931869b853768f0ba935d67" +
            "f837a66b172dd39a60ca2315f8d4e0e669bbd35cf25010000000000000002000000000000000000000000000000000000000000" +
            "00000000000000000000010d6170746f735f6163636f756e74087472616e7366657200022007968dab936c1bad187c60ce4082f" +
            "307d030d780e91e694ae03aef16aba73f300800e1f505000000000900000000000000640000000000000031f8c06500000000" +
            "01\"," +
            "\"type\":\"ed25519_signature\"" +
            "}" +
            "}"

        val actual = txBuilder.buildForSend(
            sequenceNumber = 1,
            transactionData = TransactionData(
                amount = APT_AMOUNT.copy(value = BigDecimal(1)),
                fee = Fee.Aptos(
                    amount = APT_AMOUNT.copy(value = BigDecimal(9).movePointLeft(8)),
                    gasUnitPrice = 100,
                ),
                sourceAddress = SOURCE_ADDRESS,
                destinationAddress = DESTINATION_ADDRESS,
                status = TransactionStatus.Unconfirmed,
            ),
            expirationTimestamp = 1707145265,
            signature = (
                "B5E97DB07FA0BD0E5598AA3643A9BC6F6693BDDC1A9FEC9E674A461EAA00B1931869B853768F0BA935D67F837A66" +
                    "B172DD39A60CA2315F8D4E0E669BBD35CF250100000000000000020000000000000000000000000000000000000000000000000" +
                    "0000000000000010D6170746F735F6163636F756E74087472616E7366657200022007968DAB936C1BAD187C60CE4082F307D030" +
                    "D780E91E694AE03AEF16ABA73F300800E1F505000000000900000000000000640000000000000031F8C0650000000001"
                )
                .hexToBytes(),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    private companion object {
        const val PUBLIC_KEY = "0x62e7a6a486553b56a53e89dfae3f780693e537e5b0a7ed33290780e581ca8369"
        const val SOURCE_ADDRESS = "0x1869b853768f0ba935d67f837a66b172dd39a60ca2315f8d4e0e669bbd35cf25"
        const val DESTINATION_ADDRESS = "0x07968dab936c1bad187c60ce4082f307d030d780e91e694ae03aef16aba73f30"

        val APT_AMOUNT = Amount(currencySymbol = "APT", value = null, decimals = 8, type = AmountType.Coin)
    }
}
