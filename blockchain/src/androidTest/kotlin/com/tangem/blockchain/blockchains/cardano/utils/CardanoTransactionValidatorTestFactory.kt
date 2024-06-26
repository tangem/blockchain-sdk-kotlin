package com.tangem.blockchain.blockchains.cardano.utils

import com.tangem.blockchain.blockchains.cardano.CardanoTransactionValidatorTest
import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoUnspentOutput
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.extensions.hexToBytes
import java.math.BigDecimal

internal class CardanoTransactionValidatorTestFactory {

    /** 5.66 ADA, no tokens */
    fun create_5_66_ADA(): CardanoTransactionValidatorTest.Model {
        return CardanoTransactionValidatorTest.Model(
            wallet = ONLY_ADA_WALLET.apply {
                setCoinValue(value = BigDecimal(5.66))
            },
            utxos = listOf(UTXO_2_ADA, UTXO_3_66_ADA),
        )
    }

    /** 2 ADA, [wmtValue] (default 10) WMT */
    fun create_2_ADA_and_WMT(wmtValue: BigDecimal = BigDecimal(10)): CardanoTransactionValidatorTest.Model {
        return CardanoTransactionValidatorTest.Model(
            wallet = WALLET_WITH_TOKEN.apply {
                setCoinValue(value = BigDecimal(2))
                addTokenValue(value = wmtValue, token = WMT_TOKEN)
            },
            utxos = listOf(
                UTXO_2_ADA.copy(
                    assets = listOf(
                        WMT_ASSET.copy(amount = wmtValue.movePointRight(WMT_TOKEN.decimals).toLong()),
                    ),
                ),
            ),
        )
    }

    /** 2 ADA, [wmtValue] (default 10) WMT, [agixValue] (default 10) AGIX */
    fun create_2_ADA_and_WMT_and_AGIX(
        wmtValue: BigDecimal = BigDecimal(10),
        agixValue: BigDecimal = BigDecimal(10),
    ): CardanoTransactionValidatorTest.Model {
        return CardanoTransactionValidatorTest.Model(
            wallet = WALLET_WITH_TOKEN.apply {
                setCoinValue(value = BigDecimal(2))
                addTokenValue(value = wmtValue, token = WMT_TOKEN)
                addTokenValue(value = agixValue, token = AGIX_TOKEN)
            },
            utxos = listOf(
                UTXO_2_ADA.copy(
                    assets = listOf(
                        WMT_ASSET.copy(amount = wmtValue.movePointRight(WMT_TOKEN.decimals).toLong()),
                        AGIX_ASSET.copy(amount = agixValue.movePointRight(AGIX_TOKEN.decimals).toLong()),
                    ),
                ),
            ),
        )
    }

    /** 5.66 ADA, [wmtValue] (default 10) WMT */
    fun create_5_66_ADA_and_WMT(wmtValue: BigDecimal = BigDecimal(10)): CardanoTransactionValidatorTest.Model {
        return CardanoTransactionValidatorTest.Model(
            wallet = WALLET_WITH_TOKEN.apply {
                setCoinValue(value = BigDecimal(5.66))
                addTokenValue(value = wmtValue, token = WMT_TOKEN)
            },
            utxos = listOf(
                UTXO_3_66_ADA,
                UTXO_2_ADA.copy(
                    assets = listOf(
                        WMT_ASSET.copy(amount = wmtValue.movePointRight(WMT_TOKEN.decimals).toLong()),
                    ),
                ),
            ),
        )
    }

    /** 5.66 ADA, [wmtValue] (default 10) WMT, [agixValue] (default 10) AGIX */
    fun create_5_66_ADA_and_WMT_and_AGIX(
        wmtValue: BigDecimal = BigDecimal(10),
        agixValue: BigDecimal = BigDecimal(10),
    ): CardanoTransactionValidatorTest.Model {
        return CardanoTransactionValidatorTest.Model(
            wallet = WALLET_WITH_2_TOKENS.apply {
                setCoinValue(value = BigDecimal(5.66))
                addTokenValue(value = wmtValue, token = WMT_TOKEN)
                addTokenValue(value = agixValue, token = AGIX_TOKEN)
            },
            utxos = listOf(
                UTXO_3_66_ADA,
                UTXO_2_ADA.copy(
                    assets = listOf(
                        WMT_ASSET.copy(amount = wmtValue.movePointRight(WMT_TOKEN.decimals).toLong()),
                        AGIX_ASSET.copy(amount = agixValue.movePointRight(AGIX_TOKEN.decimals).toLong()),
                    ),
                ),
            ),
        )
    }

    fun createCoinTransaction(value: BigDecimal): TransactionData {
        return TransactionData(
            amount = Amount(value = value, blockchain = Blockchain.Cardano, type = AmountType.Coin),
            fee = Fee.Common(amount = Amount(BigDecimal.ONE, Blockchain.Cardano)),
            sourceAddress = "addr1vxmeqr8j5wrpz9atx8yc5jydw5wgnrpxe7whlv542a3wvrs2dy0h4",
            destinationAddress = "addr1vxmeqr8j5wrpz9atx8yc5jydw5wgnrpxe7whlv542a3wvrs2dy0h4",
        )
    }

    fun createWMTTransaction(value: BigDecimal): TransactionData {
        return TransactionData(
            amount = Amount(
                value = value,
                blockchain = Blockchain.Cardano,
                type = AmountType.Token(token = WMT_TOKEN),
            ),
            fee = Fee.CardanoToken(
                amount = Amount(value = BigDecimal(0.013), blockchain = Blockchain.Cardano),
                minAdaValue = BigDecimal.ZERO,
            ),
            sourceAddress = "addr1vxmeqr8j5wrpz9atx8yc5jydw5wgnrpxe7whlv542a3wvrs2dy0h4",
            destinationAddress = "addr1vxmeqr8j5wrpz9atx8yc5jydw5wgnrpxe7whlv542a3wvrs2dy0h4",
        )
    }

    private companion object {

        val ONLY_ADA_WALLET by lazy {
            Wallet(
                blockchain = Blockchain.Cardano,
                addresses = setOf(),
                publicKey = Wallet.PublicKey(seedKey = "".hexToBytes(), derivationType = null),
                tokens = setOf(),
            )
        }

        val WALLET_WITH_TOKEN by lazy {
            Wallet(
                blockchain = Blockchain.Cardano,
                addresses = setOf(),
                publicKey = Wallet.PublicKey(seedKey = "".hexToBytes(), derivationType = null),
                tokens = setOf(WMT_TOKEN),
            )
        }

        val WALLET_WITH_2_TOKENS by lazy {
            Wallet(
                blockchain = Blockchain.Cardano,
                addresses = setOf(),
                publicKey = Wallet.PublicKey(seedKey = "".hexToBytes(), derivationType = null),
                tokens = setOf(WMT_TOKEN, AGIX_TOKEN),
            )
        }

        val WMT_TOKEN by lazy {
            Token(
                name = "World Mobile Token",
                symbol = "WMT",
                contractAddress = "1d7f33bd23d85e1a25d87d86fac4f199c3197a2f7afeb662a0f34e1e776f726c646d6f62696c65746f6b656e",
                decimals = 6,
                id = "world-mobile-token",
            )
        }

        val WMT_ASSET by lazy {
            CardanoUnspentOutput.Asset(
                policyID = "1d7f33bd23d85e1a25d87d86fac4f199c3197a2f7afeb662a0f34e1e",
                assetName = "776f726c646d6f62696c65746f6b656e",
                amount = 0,
            )
        }

        val AGIX_TOKEN by lazy {
            Token(
                name = "SingularityNET",
                symbol = "AGIX",
                contractAddress = "f43a62fdc3965df486de8a0d32fe800963589c41b38946602a0dc535",
                decimals = 8,
                id = "singularitynet",
            )
        }

        val AGIX_ASSET by lazy {
            CardanoUnspentOutput.Asset(
                policyID = "f43a62fdc3965df486de8a0d32fe800963589c41b38946602a0dc535",
                assetName = "41474958",
                amount = 0,
            )
        }

        val UTXO_2_ADA by lazy {
            CardanoUnspentOutput(
                address = "addr1vxmeqr8j5wrpz9atx8yc5jydw5wgnrpxe7whlv542a3wvrs2dy0h4",
                outputIndex = 0,
                amount = 2000000,
                transactionHash = byteArrayOf(
                    -31, -14, -112, 17, 29, -95, 14, -61, -25, -29, 87, 42, -121, -27, -109, -125, -56, -25, 63, -111,
                    -86, -60, 48, 90, -16, 1, 13, -126, 117, -23, -3,
                ),
                assets = emptyList(),
            )
        }

        val UTXO_3_66_ADA by lazy {
            CardanoUnspentOutput(
                address = "addr1vxmeqr8j5wrpz9atx8yc5jydw5wgnrpxe7whlv542a3wvrs2dy0h4",
                outputIndex = 1,
                amount = 3660000,
                transactionHash = byteArrayOf(
                    31, -21, -70, -62, 13, -124, -92, -81, -23, -58, 114, 70, -101, -66, 43, 1, -24, 84, -25, 48, -74,
                    39, -109, 53, -68, -9, -75, -49, -57, -89, 33, 53,
                ),
                assets = emptyList(),
            )
        }
    }
}