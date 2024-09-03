package com.tangem.blockchain.blockchains.filecoin

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinSignedTransactionBody
import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinTransactionBody
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import java.math.BigDecimal

internal class FilecoinTransactionBuilderTest {

    init {
        System.loadLibrary("TrustWalletCore")
    }

    private val txBuilder = FilecoinTransactionBuilder(
        wallet = Wallet(
            blockchain = Blockchain.Filecoin,
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
        val expected = "BEB93CCF5C85273B327AC5DCDD58CBF3066F57FC84B87CD20DC67DF69EC2D0A9".hexToBytes()

        val actual = txBuilder.buildForSign(
            nonce = 2,
            transactionData = TransactionData.Uncompiled(
                amount = FIL_AMOUNT.copy(value = BigDecimal(0.01)),
                fee = Fee.Filecoin(
                    amount = FIL_AMOUNT.copy(
                        value = BigDecimal(101225).multiply(BigDecimal(1526328))
                            .movePointLeft(18),
                    ),
                    gasUnitPrice = 101225,
                    gasLimit = 1526328,
                    gasPremium = 50612,
                ),
                sourceAddress = SOURCE_ADDRESS,
                destinationAddress = DESTINATION_ADDRESS,
                status = TransactionStatus.Unconfirmed,
            ),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun buildForSend() {
        val expected = FilecoinSignedTransactionBody(
            transactionBody = FilecoinTransactionBody(
                sourceAddress = "f1flbddhx4vwox3y3ux5bwgsgq2frzeiuvvdrjo7i",
                destinationAddress = "f1rluskhwvv5b3z36skltu4noszbc5stfihevbf2i",
                amount = "10000000000000000",
                nonce = 2,
                gasUnitPrice = "101225",
                gasLimit = 1526328,
                gasPremium = "50612",
            ),
            signature = FilecoinSignedTransactionBody.Signature(
                type = 1,
                signature = "Bogel9o9zvXUT+sC+nVpciGyHfBxWG6V4+xOawP6YrAU1OIbifvEHpRT/Elakv2X6mfUkbQzparvc2HyJBbXRwE=",
            ),
        )

        val actual = txBuilder.buildForSend(
            nonce = 2,
            transactionData = TransactionData.Uncompiled(
                amount = FIL_AMOUNT.copy(value = BigDecimal(0.01)),
                fee = Fee.Filecoin(
                    amount = FIL_AMOUNT.copy(
                        value = BigDecimal(101225).multiply(BigDecimal(1526328))
                            .movePointLeft(18),
                    ),
                    gasUnitPrice = 101225,
                    gasLimit = 1526328,
                    gasPremium = 50612,
                ),
                sourceAddress = SOURCE_ADDRESS,
                destinationAddress = DESTINATION_ADDRESS,
                status = TransactionStatus.Unconfirmed,
            ),
            signature = (
                "06881E97DA3DCEF5D44FEB02FA75697221B21DF071586E95E3EC4E6B03FA62B014D4E21B89FBC41E9453FC495A92FD97EA6" +
                    "7D491B433A5AAEF7361F22416D74701"
                ).hexToBytes(),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    private companion object {
        const val PUBLIC_KEY = "0374D0F81F42DDFE34114D533E95E6AE5FE6EA271C96F1FA505199FDC365AE9720"
        const val SOURCE_ADDRESS = "f1flbddhx4vwox3y3ux5bwgsgq2frzeiuvvdrjo7i"
        const val DESTINATION_ADDRESS = "f1rluskhwvv5b3z36skltu4noszbc5stfihevbf2i"

        val FIL_AMOUNT = Amount(currencySymbol = "FIL", value = null, decimals = 18, type = AmountType.Coin)
    }
}