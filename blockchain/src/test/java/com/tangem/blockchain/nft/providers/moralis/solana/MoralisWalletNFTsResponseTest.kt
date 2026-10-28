package com.tangem.blockchain.nft.providers.moralis.solana

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.blockchain.nft.providers.moralis.solana.network.MoralisWalletNFTsResponse
import org.junit.Test

internal class MoralisWalletNFTsResponseTest {

    private val adapter = Moshi.Builder().build()
        .adapter(MoralisWalletNFTsResponse::class.java)

    @Test
    fun `core response decodes with nullable fields`() {
        val response = requireNotNull(adapter.fromJson(MoralisSolanaFixtures.coreResponse))

        assertThat(response.cursor).isEqualTo("next-page")
        assertThat(response.meta?.failedChains).isEmpty()
        assertThat(response.result?.map { it.tokenAddress }).containsExactly(
            "CoreAsset1111111111111111111111111111111111",
            "CoreAsset2222222222222222222222222222222222",
            "Compressed33333333333333333333333333333333",
            null,
        ).inOrder()
        assertThat(response.result?.first()?.collection?.address)
            .isEqualTo("CoreCollection11111111111111111111111111111")
    }

    @Test
    fun `failed chains are decoded`() {
        val response = requireNotNull(adapter.fromJson(MoralisSolanaFixtures.failedChainResponse))

        assertThat(response.meta?.failedChains).hasSize(1)
    }
}