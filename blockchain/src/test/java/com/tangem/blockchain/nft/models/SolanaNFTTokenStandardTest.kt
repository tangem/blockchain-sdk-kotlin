package com.tangem.blockchain.nft.models

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class SolanaNFTTokenStandardTest {

    @Test
    fun `metaplex token standard index maps to the moralis name`() {
        val cases = mapOf(
            0 to "Metaplex",
            1 to null,
            2 to null,
            3 to "MetaplexEdition",
            4 to "ProgrammableNft",
            5 to "ProgrammableNftEdition",
            null to null,
        )

        cases.forEach { (index, expected) ->
            assertThat(SolanaNFTTokenStandard.fromMetaplexIndex(index)).isEqualTo(expected)
        }
    }
}