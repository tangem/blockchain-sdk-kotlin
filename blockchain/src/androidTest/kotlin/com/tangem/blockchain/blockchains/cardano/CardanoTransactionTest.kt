package com.tangem.blockchain.blockchains.cardano

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CardanoTransactionTest {

    init {
        System.loadLibrary("TrustWalletCore")
    }

    private val transactionBuilder = CardanoTransactionBuilder()

    @Before
    fun prepare() {
        val unspentOutput1 = CardanoUnspentOutput(
            address = "addr1vxmeqr8j5wrpz9atx8yc5jydw5wgnrpxe7whlv542a3wvrs2dy0h4",
            outputIndex = 0,
            amount = 2000000,
            transactionHash = byteArrayOf(
                -31, -14, -112, 17, 29, -95, 14, -61, -25, -29, 87, 42, -121, -27, -109, -125, -56, -25, 63, -111, -86,
                -60, 48, 90, -16, 1, 13, -126, 117, -23, -3,
            ),
        )

        val unspentOutput2 = CardanoUnspentOutput(
            address = "addr1vxmeqr8j5wrpz9atx8yc5jydw5wgnrpxe7whlv542a3wvrs2dy0h4",
            outputIndex = 1,
            amount = 3665244,
            transactionHash = byteArrayOf(
                31, -21, -70, -62, 13, -124, -92, -81, -23, -58, 114, 70, -101, -66, 43, 1, -24, 84, -25, 48, -74, 39,
                -109, 53, -68, -9, -75, -49, -57, -89, 33, 53,
            ),
        )

        transactionBuilder.update(
            listOf(unspentOutput1, unspentOutput2),
        )
    }

    @Test
    fun testBuildForSign() {
        val result = transactionBuilder.buildForSign(
            TransactionData(
                amount = Amount(
                    currencySymbol = "ADA",
                    value = BigDecimal.valueOf(1),
                    decimals = 6,
                    type = AmountType.Coin,
                ),
                fee = Fee.Common(
                    amount = Amount(
                        currencySymbol = "ADA",
                        value = BigDecimal.valueOf(0.167378),
                        decimals = 6,
                        type = AmountType.Coin,
                    ),
                ),
                sourceAddress = "addr1vxmeqr8j5wrpz9atx8yc5jydw5wgnrpxe7whlv542a3wvrs2dy0h4",
                destinationAddress = "addr1q90uh2eawrdc9vaemftgd50l28yrh9lqxtjjh4z6dnn0u7ggasexxdyyk9f05atygnjlccsjs" +
                    "ggtc87hhqjna32fpv5qeq96ls",
            ),
        )

        val expected = byteArrayOf(
            -51, -93, -4, -71, 127, 18, 21, 124, 91, 72, -67, 61, -67, -69, -104, 85, 9, -104, 90, -42, -66, -9, -95,
            89, 74, -22, 47, -64, -121, 85, -111, 21,
        )

        assertArrayEquals(result, expected)
    }

    @Test
    fun testBuildForSend() {
        val result = transactionBuilder.buildForSend(
            transaction = TransactionData(
                amount = Amount(
                    currencySymbol = "ADA",
                    value = BigDecimal.valueOf(1),
                    decimals = 6,
                    type = AmountType.Coin,
                ),
                fee = Fee.Common(
                    amount = Amount(
                        currencySymbol = "ADA",
                        value = BigDecimal.valueOf(0.167378),
                        decimals = 6,
                        type = AmountType.Coin,
                    ),
                ),
                sourceAddress = "addr1vxmeqr8j5wrpz9atx8yc5jydw5wgnrpxe7whlv542a3wvrs2dy0h4",
                destinationAddress = "addr1q90uh2eawrdc9vaemftgd50l28yrh9lqxtjjh4z6dnn0u7ggasexxdyyk9f05atygnjlccsjs" +
                    "ggtc87hhqjna32fpv5qeq96ls",
            ),
            signatureInfo = SignatureInfo(
                signature = byteArrayOf(
                    -84, -74, 30, -114, -57, -23, -31, 75, 27, 121, 73, -103, 92, -28, -25, 111, -84, 63, 77, 92, -56,
                    87, -108, -42, -78, -78, -105, 121, 59, -14, -51, -85, -67, -117, -94, -114, 41, 71, -93, -65, 18,
                    -29, 100, 117, 112, -111, 123, -41, -107, -107, 60, -42, -74, 49, -35, 46, 9, -87, -15, -107, 35,
                    -101, 116, 13,
                ),
                publicKey = byteArrayOf(
                    100, -53, -68, -38, -80, 91, 82, -56, -63, -117, -14, 123, -61, 102, 28, 55, -54, -50, 123, -121,
                    -97, 66, 87, 68, -82, -69, -93, 35, -24, 80, -37, -13,
                ),
            ),
        )

        val expected = byteArrayOf(
            -125, -92, 0, -127, -126, 88, 32, 31, -21, -70, -62, 13, -124, -92, -81, -23, -58, 114, 70, -101, -66, 43,
            1, -24, 84, -25, 48, -74, 39, -109, 53, -68, -9, -75, -49, -57, -89, 33, 53, 1, 1, -126, -126, 88, 57, 1,
            95, -53, -85, 61, 112, -37, -126, -77, -71, -38, 86, -122, -47, -1, 81, -56, 59, -105, -32, 50, -27, 43,
            -44, 90, 108, -26, -2, 121, 8, -20, 50, 99, 52, -124, -79, 82, -6, 117, 100, 68, -27, -4, 98, 18, -126, 16,
            -68, 31, -41, -72, 37, 62, -59, 73, 11, 40, 26, 0, 15, 66, 64, -126, 88, 29, 97, -73, -112, 12, -14, -93,
            -122, 17, 23, -85, 49, -55, -118, 72, -115, 117, 28, -119, -116, 38, -49, -99, 127, -78, -107, 87, 98, -26,
            14, 26, 0, 38, 29, 74, 2, 26, 0, 2, -115, -46, 3, 26, 11, 83, 43, -128, -95, 0, -127, -126, 88, 32, 100,
            -53, -68, -38, -80, 91, 82, -56, -63, -117, -14, 123, -61, 102, 28, 55, -54, -50, 123, -121, -97, 66, 87,
            68, -82, -69, -93, 35, -24, 80, -37, -13, 88, 64, -84, -74, 30, -114, -57, -23, -31, 75, 27, 121, 73, -103,
            92, -28, -25, 111, -84, 63, 77, 92, -56, 87, -108, -42, -78, -78, -105, 121, 59, -14, -51, -85, -67, -117,
            -94, -114, 41, 71, -93, -65, 18, -29, 100, 117, 112, -111, 123, -41, -107, -107, 60, -42, -74, 49, -35, 46,
            9, -87, -15, -107, 35, -101, 116, 13, -10,
        )

        assertArrayEquals(result, expected)
    }
}
