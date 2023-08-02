package com.tangem.blockchain.blockchains.cosmos

import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Wallet
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CosmosTransactionTest {

    init {
        System.loadLibrary("TrustWalletCore")
    }

    private val seedKey = byteArrayOf(
        2, -30, -43, 25, 85, 93, 103, -26, 0, -37, 27, -102, 51, 106, 41, -109, 82,
        -21, -26, -73, -71, -28, 127, 110, 110, -99, 89, 94, -7, 85, 25, -20, -90
    )

    private val derivedKey = byteArrayOf(
        2, -40, -68, 33, -71, -109, 85, -40, 123, 22, -61, -40, 47, 23, -112, -48, 74,
        -84, -43, 87, -75, 56, -93, -89, 33, 57, -21, -12, -31, 71, 111, 49, 26
    )

    private val signature = byteArrayOf(
        65, 26, -22, 18, -112, -122, -4, -4, 104, -53, 89, 92, -113, 58, 123, 88, -5, 17, 45, 1, 15, 25, 75, 2, 48, 81, 105, 70, -39, 78, 1, -73, 67, 106, 51, 104, 91, -102, -125, 97, -49, 90, 55, 62, -48, 110, -14, 31, -15, -92, 19, -62, -110, 51, -86, -53, -55, -93, 122, 108, -68, 72, -56, 6
    )

    private val publicKey = Wallet.PublicKey(seedKey, derivedKey, null)

    private val transactionBuilder = CosmosTransactionBuilder(
        cosmosChain = CosmosChain.Cosmos(true),
        publicKey = publicKey
    )

    @Test
    fun testBuildForSign() {
        val actual = transactionBuilder.buildForSign(
            amount = Amount(
                currencySymbol = "ATOM",
                value = "0.05".toBigDecimal(),
                decimals = 6,
                type = AmountType.Coin

            ),
            source = "cosmos1tqksn8j4kj0feed2sglhfujp5amkndyac4z8jy",
            destination = "cosmos1z56v8wqvgmhm3hmnffapxujvd4w4rkw6cxr8xy",
            accountNumber = 726521,
            sequenceNumber = 17,
            feeAmount = Amount(
                currencySymbol = "ATOM",
                value = BigDecimal.valueOf(0.002717),
                decimals = 6,
                type = AmountType.Coin
            ),
            gas = 108700,
            extras = null
        )


        val expected = byteArrayOf(
            45, 2, 116, -20, -35, -15, -125, 119, 12, 92, 28, -100, -95, -28, 104, 119, 99,
            -72, -53, 70, -83, -123, -102, -89, -61, -103, 34, -19, 52, 101, 63, -20
        )

        assertArrayEquals(actual, expected)
    }

    @Test
    fun testBuildForSend() {
        val message = transactionBuilder.buildForSend(
            amount = Amount(
                currencySymbol = "ATOM",
                value = "0.05".toBigDecimal(),
                decimals = 6,
                type = AmountType.Coin

            ),
            source = "cosmos1tqksn8j4kj0feed2sglhfujp5amkndyac4z8jy",
            destination = "cosmos1z56v8wqvgmhm3hmnffapxujvd4w4rkw6cxr8xy",
            accountNumber = 726521,
            sequenceNumber = 16,
            feeAmount = Amount(
                currencySymbol = "ATOM",
                value = BigDecimal.valueOf(0.002717),
                decimals = 6,
                type = AmountType.Coin
            ),
            gas = 108700,
            extras = null,
            signature = signature
        )

        val expected =
            "{\"mode\":\"BROADCAST_MODE_SYNC\",\"tx_bytes\":\"CpEBCo4BChwvY29zbW9zLmJhbmsudjFiZXRhMS5Nc2dTZW5kEm4KLWNvc21vczF0cWtzbjhqNGtqMGZlZWQyc2dsaGZ1anA1YW1rbmR5YWM0ejhqeRItY29zbW9zMXo1NnY4d3F2Z21obTNobW5mZmFweHVqdmQ0dzRya3c2Y3hyOHh5Gg4KBXVhdG9tEgU1MDAwMBJnClAKRgofL2Nvc21vcy5jcnlwdG8uc2VjcDI1NmsxLlB1YktleRIjCiEC2LwhuZNV2HsWw9gvF5DQSqzVV7U4o6chOev04UdvMRoSBAoCCAEYEBITCg0KBXVhdG9tEgQyNzE3EJzRBhpAQRrqEpCG/Pxoy1lcjzp7WPsRLQEPGUsCMFFpRtlOAbdDajNoW5qDYc9aNz7QbvIf8aQTwpIzqsvJo3psvEjIBg==\"}"
        val actual = message

        assertEquals(expected, actual)
    }

}