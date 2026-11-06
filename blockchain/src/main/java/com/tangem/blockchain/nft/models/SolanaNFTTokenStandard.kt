package com.tangem.blockchain.nft.models

/**
 * Token standards of Solana NFTs as the Moralis wallet NFTs API names them. The legacy Moralis endpoint returns the
 * Metaplex `TokenStandard` enum index instead; [fromMetaplexIndex] brings it to the same names.
 */
object SolanaNFTTokenStandard {
    const val METAPLEX = "Metaplex"
    const val METAPLEX_EDITION = "MetaplexEdition"
    const val PROGRAMMABLE_NFT = "ProgrammableNft"
    const val PROGRAMMABLE_NFT_EDITION = "ProgrammableNftEdition"
    const val TOKEN_2022 = "Token2022"
    const val MPL_CORE = "MplCore"
    const val COMPRESSED = "Compressed"

    private const val NON_FUNGIBLE = 0
    private const val NON_FUNGIBLE_EDITION = 3
    private const val PROGRAMMABLE_NON_FUNGIBLE = 4
    private const val PROGRAMMABLE_NON_FUNGIBLE_EDITION = 5

    fun fromMetaplexIndex(index: Int?): String? = when (index) {
        NON_FUNGIBLE -> METAPLEX
        NON_FUNGIBLE_EDITION -> METAPLEX_EDITION
        PROGRAMMABLE_NON_FUNGIBLE -> PROGRAMMABLE_NFT
        PROGRAMMABLE_NON_FUNGIBLE_EDITION -> PROGRAMMABLE_NFT_EDITION
        else -> null
    }
}