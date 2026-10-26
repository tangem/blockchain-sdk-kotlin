package com.tangem.blockchain.blockchains.sui.network

import com.tangem.blockchain.common.Blockchain
import java.math.BigDecimal

internal object SuiConstants {

    const val COIN_TYPE = "0x2::sui::SUI"
    val SUI_GAS_BUDGET_MAX_VALUE = BigDecimal(50_000_000_000)

    private const val MAINNET_CHAIN_IDENTIFIER = "4btiuiMPvEENsttpZC7CZ53DruC3MAgfznDbASZ7DR6S"
    private const val TESTNET_CHAIN_IDENTIFIER = "69WiPg3DAQiwdxfncX6wYQ2siKwAe6L9BZthQea3JNMD"

    private const val COIN_TYPE_LONG = "0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI"

    /** Digest of the genesis checkpoint, which `ValidDuring` transactions name to prevent cross-chain replay */
    fun chainIdentifierOf(blockchain: Blockchain): String = when (blockchain) {
        Blockchain.SuiTestnet -> TESTNET_CHAIN_IDENTIFIER
        else -> MAINNET_CHAIN_IDENTIFIER
    }

    /** Extracts `T` from a `0x2::coin::Coin<T>` object type, see [normalizeCoinType] */
    fun coinTypeOf(coinObjectType: String): String {
        return normalizeCoinType(coinObjectType.substringAfter(delimiter = "<").removeSuffix(suffix = ">"))
    }

    /**
     * gRPC pads every address to 32 bytes, while the SDK keys the native coin by the short [COIN_TYPE]; token types
     * are padded by the wallet manager anyway.
     */
    fun normalizeCoinType(coinType: String): String = if (coinType == COIN_TYPE_LONG) COIN_TYPE else coinType
}