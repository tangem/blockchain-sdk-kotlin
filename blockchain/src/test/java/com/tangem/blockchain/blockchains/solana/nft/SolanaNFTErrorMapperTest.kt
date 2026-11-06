package com.tangem.blockchain.blockchains.solana.nft

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.BlockchainSdkError
import org.junit.Test

internal class SolanaNFTErrorMapperTest {

    @Test
    fun `known core program errors become nft errors`() {
        val cases = mapOf(
            "0x1a" to BlockchainSdkError.Solana.NFTNotOwner,
            "0x9" to BlockchainSdkError.Solana.NFTTransferRejected,
            "0x13" to BlockchainSdkError.Solana.NFTInvalidCollection,
            "0x19" to BlockchainSdkError.Solana.NFTInvalidCollection,
        )

        cases.forEach { (code, expected) ->
            val error = BlockchainSdkError.Solana.Api(
                Exception("Transaction simulation failed: Error processing Instruction 2: custom program error: $code"),
            )
            assertThat(SolanaNFTErrorMapper.mapCoreError(error)).isEqualTo(expected)
        }
    }

    @Test
    fun `other errors are kept`() {
        val unknown = BlockchainSdkError.Solana.Api(Exception("custom program error: 0x22"))
        val other = BlockchainSdkError.FailedToBuildTx

        assertThat(SolanaNFTErrorMapper.mapCoreError(unknown)).isSameInstanceAs(unknown)
        assertThat(SolanaNFTErrorMapper.mapCoreError(other)).isSameInstanceAs(other)
    }
}