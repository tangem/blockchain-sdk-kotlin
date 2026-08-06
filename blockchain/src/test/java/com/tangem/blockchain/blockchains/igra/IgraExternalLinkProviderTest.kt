package com.tangem.blockchain.blockchains.igra

import com.google.common.truth.Truth
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchain.externallinkprovider.providers.IgraExternalLinkProvider
import org.junit.Test

/**
 * Tests for IgraExternalLinkProvider
 */
internal class IgraExternalLinkProviderTest {

    @Test
    fun `IgraExternalLinkProvider mainnet has correct explorer URL`() {
        val provider = IgraExternalLinkProvider(isTestnet = false)

        Truth.assertThat(provider.explorerBaseUrl).isEqualTo("https://explorer.igralabs.com/")
    }

    @Test
    fun `IgraExternalLinkProvider testnet has correct explorer URL`() {
        val provider = IgraExternalLinkProvider(isTestnet = true)

        Truth.assertThat(provider.explorerBaseUrl)
            .isEqualTo("https://explorer.galleon-testnet.igralabs.com/")
    }

    @Test
    fun `IgraExternalLinkProvider mainnet generates correct wallet URL`() {
        val provider = IgraExternalLinkProvider(isTestnet = false)
        val walletAddress = "0xF010f34afF5Ddd52aAF7dA02F1D1a2e0cB1f9b9C"

        val url = provider.explorerUrl(walletAddress, null)

        Truth.assertThat(url).isEqualTo("https://explorer.igralabs.com/address/$walletAddress")
    }

    @Test
    fun `IgraExternalLinkProvider testnet generates correct transaction URL`() {
        val provider = IgraExternalLinkProvider(isTestnet = true)
        val txHash = "0xcd340da084c139e1e127b7ba4817663121ac4a2fe0a556d454c4ea9ca9f74d2f"

        val txState = provider.getExplorerTxUrl(txHash)

        Truth.assertThat(txState).isInstanceOf(TxExploreState.Url::class.java)
        val url = (txState as TxExploreState.Url).url
        Truth.assertThat(url).isEqualTo("https://explorer.galleon-testnet.igralabs.com/tx/$txHash")
    }

    @Test
    fun `IgraExternalLinkProvider has no testnet faucet`() {
        Truth.assertThat(IgraExternalLinkProvider(isTestnet = true).testNetTopUpUrl).isNull()
        Truth.assertThat(IgraExternalLinkProvider(isTestnet = false).testNetTopUpUrl).isNull()
    }

    @Test
    fun `IgraExternalLinkProvider ignores contract address in explorer URL`() {
        val provider = IgraExternalLinkProvider(isTestnet = false)
        val walletAddress = "0xF010f34afF5Ddd52aAF7dA02F1D1a2e0cB1f9b9C"
        val contractAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"

        val url = provider.explorerUrl(walletAddress, contractAddress)

        Truth.assertThat(url).isEqualTo("https://explorer.igralabs.com/address/$walletAddress")
        Truth.assertThat(url).doesNotContain(contractAddress)
    }
}