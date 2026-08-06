package com.tangem.blockchain.blockchains.cosmos

import com.google.protobuf.ByteString
import com.tangem.blockchain.blockchains.cosmos.network.CosmosChain
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Wallet
import com.tangem.common.extensions.hexToBytes
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import wallet.core.java.AnySigner
import wallet.core.jni.AnyAddress
import wallet.core.jni.CoinType
import wallet.core.jni.Curve
import wallet.core.jni.PrivateKey
import wallet.core.jni.proto.Cosmos
import java.math.BigDecimal

class CosmosTransactionTest {

    init {
        System.loadLibrary("TrustWalletCore")
    }

    private val seedKey = byteArrayOf(
        2, -30, -43, 25, 85, 93, 103, -26, 0, -37, 27, -102, 51, 106, 41, -109, 82, -21, -26, -73, -71, -28, 127, 110,
        110, -99, 89, 94, -7, 85, 25, -20, -90,
    )

    private val signature = byteArrayOf(
        65, 26, -22, 18, -112, -122, -4, -4, 104, -53, 89, 92, -113, 58, 123, 88, -5, 17, 45, 1, 15, 25, 75, 2, 48, 81,
        105, 70, -39, 78, 1, -73, 67, 106, 51, 104, 91, -102, -125, 97, -49, 90, 55, 62, -48, 110, -14, 31, -15, -92,
        19, -62, -110, 51, -86, -53, -55, -93, 122, 108, -68, 72, -56, 6,
    )

    private val publicKey = Wallet.PublicKey(seedKey, null)

    private val transactionBuilder = CosmosTransactionBuilder(
        cosmosChain = CosmosChain.Cosmos(true),
        publicKey = publicKey,
    )

    @Test
    fun testBuildForSign() {
        val actual = transactionBuilder.buildForSign(
            amount = Amount(
                currencySymbol = "ATOM",
                value = "0.05".toBigDecimal(),
                decimals = 6,
                type = AmountType.Coin,
            ),
            source = "cosmos1tqksn8j4kj0feed2sglhfujp5amkndyac4z8jy",
            destination = "cosmos1z56v8wqvgmhm3hmnffapxujvd4w4rkw6cxr8xy",
            accountNumber = 726521,
            sequenceNumber = 17,
            feeAmount = Amount(
                currencySymbol = "ATOM",
                value = BigDecimal.valueOf(0.002717),
                decimals = 6,
                type = AmountType.Coin,
            ),
            gas = 108700,
            extras = null,
        )

        val expected = byteArrayOf(
            -9, -112, -18, 11, 42, -90, -110, 117, -36, -120, -124, 97, 46, 0, 50, -124, 112, 119, -62, -100, 115,
            -39, -52, -32, 41, -106, -123, -53, -127, -64, 98, 119,
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
                type = AmountType.Coin,
            ),
            source = "cosmos1tqksn8j4kj0feed2sglhfujp5amkndyac4z8jy",
            destination = "cosmos1z56v8wqvgmhm3hmnffapxujvd4w4rkw6cxr8xy",
            accountNumber = 726521,
            sequenceNumber = 16,
            feeAmount = Amount(
                currencySymbol = "ATOM",
                value = BigDecimal.valueOf(0.002717),
                decimals = 6,
                type = AmountType.Coin,
            ),
            gas = 108700,
            extras = null,
            signature = signature,
        )

        val expected = "{\"mode\":\"BROADCAST_MODE_SYNC\",\"tx_bytes\":\"CpEBCo4BChwvY29zbW9zLmJhbmsudjFiZXRhMS5Nc2d" +
            "TZW5kEm4KLWNvc21vczF0cWtzbjhqNGtqMGZlZWQyc2dsaGZ1anA1YW1rbmR5YWM0ejhqeRItY29zbW9zMXo1NnY4d3F2Z21obTNobW" +
            "5mZmFweHVqdmQ0dzRya3c2Y3hyOHh5Gg4KBXVhdG9tEgU1MDAwMBJnClAKRgofL2Nvc21vcy5jcnlwdG8uc2VjcDI1NmsxLlB1Yktle" +
            "RIjCiEC4tUZVV1n5gDbG5ozaimTUuvmt7nkf25unVle+VUZ7KYSBAoCCAEYEBITCg0KBXVhdG9tEgQyNzE3EJzRBhpAQRrqEpCG/Pxo" +
            "y1lcjzp7WPsRLQEPGUsCMFFpRtlOAbdDajNoW5qDYc9aNz7QbvIf8aQTwpIzqsvJo3psvEjIBg==\"}"

        assertEquals(expected, message)
    }

    /**
     * Fully independent verification of [CosmosTransactionBuilder] that relies on NEITHER the golden
     * base64 strings above NOR the stale `signature` fixture.
     *
     * The same Cosmos transaction is produced two different ways and compared byte-for-byte:
     *  - reference: wallet-core's canonical one-shot [AnySigner.sign];
     *  - builder:   the Tangem hardware-wallet split flow
     *               buildForSign -> external secp256k1 sign -> buildForSend.
     *
     * A real private key drives both paths, so they sign the same sign-doc. secp256k1 signing here is
     * deterministic (RFC 6979), so the two signed transactions must be identical. If buildForSign or
     * buildForSend assembled the transaction incorrectly, the outputs would diverge.
     */
    @Test
    fun testBuilderReproducesAnySignerTransaction() {
        val privateKey = PrivateKey("80e81ea269e66a0a05b11236df7919fb7fbeedba87452d667489d7403a02f005".hexToBytes())
        val compressedPublicKey = privateKey.getPublicKeySecp256k1(true)
        val source = AnyAddress(compressedPublicKey, CoinType.COSMOS).description()
        val destination = "cosmos1zt50azupanqlfam5afhv3hexwyutnukeh4c573"

        val accountNumber = 1037L
        val sequenceNumber = 8L
        val gas = 200000L
        val denom = "uatom"
        val chain = CosmosChain.Cosmos(testnet = true)

        val amount = Amount(currencySymbol = "ATOM", value = BigDecimal("0.05"), decimals = 6, type = AmountType.Coin)
        val fee = Amount(currencySymbol = "ATOM", value = BigDecimal("0.002717"), decimals = 6, type = AmountType.Coin)

        // Path A — canonical wallet-core signer (the independent "other mechanism"): 0.05 ATOM and a
        // 0.002717 ATOM fee expressed in uatom (6 decimals), signed in one shot with the private key.
        val referenceInput = Cosmos.SigningInput.newBuilder()
            .setMode(Cosmos.BroadcastMode.SYNC)
            .setSigningMode(Cosmos.SigningMode.Protobuf)
            .setAccountNumber(accountNumber)
            .setChainId(chain.chainId)
            .setSequence(sequenceNumber)
            .addMessages(
                Cosmos.Message.newBuilder().setSendCoinsMessage(
                    Cosmos.Message.Send.newBuilder()
                        .setFromAddress(source)
                        .setToAddress(destination)
                        .addAmounts(Cosmos.Amount.newBuilder().setAmount("50000").setDenom(denom)),
                ),
            )
            .setFee(
                Cosmos.Fee.newBuilder()
                    .setGas(gas)
                    .addAmounts(Cosmos.Amount.newBuilder().setAmount("2717").setDenom(denom)),
            )
            .setPrivateKey(ByteString.copyFrom(privateKey.data()))
            .build()
        val reference = AnySigner.sign(referenceInput, CoinType.COSMOS, Cosmos.SigningOutput.parser()).serialized

        // Path B — the Tangem builder's split flow, signing the produced sighash with the SAME key.
        val builder = CosmosTransactionBuilder(
            publicKey = Wallet.PublicKey(compressedPublicKey.data(), null),
            cosmosChain = chain,
        )
        val sigHash = builder.buildForSign(
            amount = amount,
            source = source,
            destination = destination,
            accountNumber = accountNumber,
            sequenceNumber = sequenceNumber,
            feeAmount = fee,
            gas = gas,
            extras = null,
        )
        // secp256k1 sign() returns r || s || v (65 bytes); Cosmos uses the 64-byte r || s.
        val externalSignature = privateKey.sign(sigHash, Curve.SECP256K1).copyOf(newSize = 64)
        val built = builder.buildForSend(
            amount = amount,
            source = source,
            destination = destination,
            accountNumber = accountNumber,
            sequenceNumber = sequenceNumber,
            feeAmount = fee,
            gas = gas,
            extras = null,
            signature = externalSignature,
        )

        assertEquals(reference, built)
    }
}