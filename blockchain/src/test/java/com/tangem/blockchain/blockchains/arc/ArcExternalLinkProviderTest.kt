package com.tangem.blockchain.blockchains.arc

import com.google.common.truth.Truth
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchain.externallinkprovider.providers.ArcExternalLinkProvider
import org.junit.Test

/**
 * Tests for ArcExternalLinkProvider
 */
internal class ArcExternalLinkProviderTest {

    @Test
    fun `ArcExternalLinkProvider mainnet has correct explorer URL`() {
        val provider = ArcExternalLinkProvider(isTestnet = false)

        Truth.assertThat(provider.explorerBaseUrl).isEqualTo("https://explorer.arc.io/")
        Truth.assertThat(provider.explorerUrl(walletAddress = ADDRESS, contractAddress = null))
            .isEqualTo("https://explorer.arc.io/address/$ADDRESS")
        Truth.assertThat(provider.getExplorerTxUrl(TX_HASH))
            .isEqualTo(TxExploreState.Url("https://explorer.arc.io/tx/$TX_HASH"))
    }

    @Test
    fun `ArcExternalLinkProvider testnet has correct explorer URL`() {
        val provider = ArcExternalLinkProvider(isTestnet = true)

        Truth.assertThat(provider.explorerBaseUrl).isEqualTo("https://testnet.arcscan.app/")
        Truth.assertThat(provider.explorerUrl(walletAddress = ADDRESS, contractAddress = null))
            .isEqualTo("https://testnet.arcscan.app/address/$ADDRESS")
        Truth.assertThat(provider.getExplorerTxUrl(TX_HASH))
            .isEqualTo(TxExploreState.Url("https://testnet.arcscan.app/tx/$TX_HASH"))
    }

    @Test
    fun `ArcExternalLinkProvider points to Circle faucet`() {
        Truth.assertThat(ArcExternalLinkProvider(isTestnet = true).testNetTopUpUrl)
            .isEqualTo("https://faucet.circle.com/")
    }

    private companion object {
        const val ADDRESS = "0x52bb4012854f808CF9BAbd855e44E506dAf6C077"
        const val TX_HASH = "0x5a2b1e0a5b5f9f0f2a1b3c4d5e6f708192a3b4c5d6e7f8091a2b3c4d5e6f7081"
    }
}