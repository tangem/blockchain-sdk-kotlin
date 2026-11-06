package com.tangem.blockchain.blockchains.solana.nft

import com.tangem.blockchain.blockchains.solana.alt.borsh.SolanaBorshException
import com.tangem.blockchain.blockchains.solana.alt.borsh.SolanaBorshReader
import org.p2p.solanaj.core.PublicKey

internal data class TokenMetadata(val tokenStandard: Int?, val ruleSet: PublicKey?) {
    val isProgrammable: Boolean get() = tokenStandard == PROGRAMMABLE_NFT || tokenStandard == PROGRAMMABLE_NFT_EDITION

    private companion object {
        const val PROGRAMMABLE_NFT = 4
        const val PROGRAMMABLE_NFT_EDITION = 5
    }
}

internal enum class TokenRecordState { Unlocked, Locked, Listed }

/**
 * Reads Token Metadata accounts. Fields after `edition_nonce` are read leniently, the way the program's own
 * `meta_deser_unchecked` does: old NFTs carry garbage in the tail, and failing on it would break their transfer.
 * `collection_details` precedes `programmable_config` and is easy to miss: skipping it shifts the read onto bytes
 * that still look like a pubkey.
 */
internal object TokenMetadataParser {

    private const val METADATA_V1 = 4
    private const val TOKEN_RECORD = 11
    private const val PROGRAMMABLE_CONFIG_V1 = 0
    private const val SELLER_FEE_SIZE = 2
    private const val CREATOR_SIZE = SolanaBorshReader.PUBLIC_KEY_SIZE + 2
    private const val MAX_TOKEN_STANDARD = 5
    private const val MAX_USE_METHOD = 2
    private const val USES_COUNTERS_SIZE = 8 + 8
    private const val COLLECTION_DETAILS_SIZE = 1 + 8

    fun parse(data: ByteArray): TokenMetadata? = runCatching {
        val reader = SolanaBorshReader(data)
        if (reader.u8() != METADATA_V1) throw SolanaBorshException("Not a Metadata account")

        reader.skip(2 * SolanaBorshReader.PUBLIC_KEY_SIZE)
        repeat(times = 3) { reader.skipString() }
        reader.skip(SELLER_FEE_SIZE)
        reader.option { creators -> creators.vector { it.skip(CREATOR_SIZE) } }
        reader.skip(2)

        reader.option { it.u8() }
        // The program drops token_standard, collection and uses together when any of them fails to parse: a
        // standard read on its own would call a garbage-tailed legacy NFT programmable and make it unsendable.
        val tokenStandard = runCatching {
            val standard = reader.option { it.u8().also(::requireTokenStandard) }
            reader.option { collection ->
                collection.bool()
                collection.skip(SolanaBorshReader.PUBLIC_KEY_SIZE)
            }
            reader.option { uses ->
                if (uses.u8() > MAX_USE_METHOD) throw SolanaBorshException("Unknown use method")
                uses.skip(USES_COUNTERS_SIZE)
            }
            standard
        }.getOrNull()
        runCatching { reader.option { it.skip(COLLECTION_DETAILS_SIZE) } }
        val ruleSet = runCatching {
            reader.option { config ->
                if (config.u8() != PROGRAMMABLE_CONFIG_V1) throw SolanaBorshException("Unknown programmable config")
                config.option { it.publicKey() }
            }
        }.getOrNull()

        TokenMetadata(tokenStandard = tokenStandard, ruleSet = ruleSet)
    }.getOrNull()

    private fun requireTokenStandard(value: Int) {
        if (value > MAX_TOKEN_STANDARD) throw SolanaBorshException("Unknown token standard")
    }

    fun parseTokenRecordState(data: ByteArray): TokenRecordState? = runCatching {
        val reader = SolanaBorshReader(data)
        if (reader.u8() != TOKEN_RECORD) throw SolanaBorshException("Not a TokenRecord account")
        reader.skip(1)
        TokenRecordState.entries[reader.u8()]
    }.getOrNull()
}