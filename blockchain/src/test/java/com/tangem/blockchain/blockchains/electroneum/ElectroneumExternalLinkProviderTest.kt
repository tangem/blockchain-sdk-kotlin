package com.tangem.blockchain.blockchains.electroneum

import com.google.common.truth.Truth
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchain.externallinkprovider.providers.ElectroneumExternalLinkProvider
import org.junit.Test

/**
 * Tests for ElectroneumExternalLinkProvider
 */
internal class ElectroneumExternalLinkProviderTest {

    @Test
    fun `ElectroneumExternalLinkProvider mainnet has correct explorer URL`() {
        val provider = ElectroneumExternalLinkProvider(isTestnet = false)

        Truth.assertThat(provider.explorerBaseUrl).isEqualTo("https://blockexplorer.electroneum.com/")
    }

    @Test
    fun `ElectroneumExternalLinkProvider testnet has correct explorer URL`() {
        val provider = ElectroneumExternalLinkProvider(isTestnet = true)

        Truth.assertThat(provider.explorerBaseUrl)
            .isEqualTo("https://testnet-blockexplorer.electroneum.com/")
    }

    @Test
    fun `ElectroneumExternalLinkProvider mainnet generates correct wallet URL`() {
        val provider = ElectroneumExternalLinkProvider(isTestnet = false)
        val walletAddress = "0xF010f34afF5Ddd52aAF7dA02F1D1a2e0cB1f9b9C"

        val url = provider.explorerUrl(walletAddress, null)

        Truth.assertThat(url).isEqualTo("https://blockexplorer.electroneum.com/address/$walletAddress")
    }

    @Test
    fun `ElectroneumExternalLinkProvider testnet generates correct transaction URL`() {
        val provider = ElectroneumExternalLinkProvider(isTestnet = true)
        val txHash = "0xcd340da084c139e1e127b7ba4817663121ac4a2fe0a556d454c4ea9ca9f74d2f"

        val txState = provider.getExplorerTxUrl(txHash)

        Truth.assertThat(txState).isInstanceOf(TxExploreState.Url::class.java)
        val url = (txState as TxExploreState.Url).url
        Truth.assertThat(url).isEqualTo("https://testnet-blockexplorer.electroneum.com/tx/$txHash")
    }

    @Test
    fun `ElectroneumExternalLinkProvider has no testnet faucet`() {
        Truth.assertThat(ElectroneumExternalLinkProvider(isTestnet = true).testNetTopUpUrl).isNull()
        Truth.assertThat(ElectroneumExternalLinkProvider(isTestnet = false).testNetTopUpUrl).isNull()
    }

    @Test
    fun `ElectroneumExternalLinkProvider ignores contract address in explorer URL`() {
        val provider = ElectroneumExternalLinkProvider(isTestnet = false)
        val walletAddress = "0xF010f34afF5Ddd52aAF7dA02F1D1a2e0cB1f9b9C"
        val contractAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"

        val url = provider.explorerUrl(walletAddress, contractAddress)

        Truth.assertThat(url).isEqualTo("https://blockexplorer.electroneum.com/address/$walletAddress")
        Truth.assertThat(url).doesNotContain(contractAddress)
    }
}