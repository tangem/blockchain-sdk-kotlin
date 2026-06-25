package com.tangem.blockchain.externallinkprovider.providers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.externallinkprovider.TxExploreState
import org.junit.Test

internal class TONExternalLinkProviderTest {

    @Test
    fun `mainnet has correct explorer base URL`() {
        val provider = TONExternalLinkProvider(isTestnet = false)

        assertThat(provider.explorerBaseUrl).isEqualTo("https://tonviewer.com/")
    }

    @Test
    fun `testnet has correct explorer base URL`() {
        val provider = TONExternalLinkProvider(isTestnet = true)

        assertThat(provider.explorerBaseUrl).isEqualTo("https://testnet.tonviewer.com/")
    }

    @Test
    fun `mainnet has no testnet top up URL`() {
        val provider = TONExternalLinkProvider(isTestnet = false)

        assertThat(provider.testNetTopUpUrl).isNull()
    }

    @Test
    fun `testnet has correct top up URL`() {
        val provider = TONExternalLinkProvider(isTestnet = true)

        assertThat(provider.testNetTopUpUrl).isEqualTo("https://t.me/testgiver_ton_bot")
    }

    @Test
    fun `mainnet generates correct wallet URL`() {
        val provider = TONExternalLinkProvider(isTestnet = false)
        val walletAddress = "EQwalletAddress"

        val url = provider.explorerUrl(walletAddress, contractAddress = null)

        assertThat(url).isEqualTo("https://tonviewer.com/$walletAddress")
    }

    @Test
    fun `testnet generates correct wallet URL`() {
        val provider = TONExternalLinkProvider(isTestnet = true)
        val walletAddress = "EQwalletAddress"

        val url = provider.explorerUrl(walletAddress, contractAddress = null)

        assertThat(url).isEqualTo("https://testnet.tonviewer.com/$walletAddress")
    }

    @Test
    fun `explorer URL ignores contract address`() {
        val provider = TONExternalLinkProvider(isTestnet = false)
        val walletAddress = "EQwalletAddress"
        val contractAddress = "EQcontractAddress"

        val url = provider.explorerUrl(walletAddress, contractAddress)

        assertThat(url).isEqualTo("https://tonviewer.com/$walletAddress")
        assertThat(url).doesNotContain(contractAddress)
    }

    @Test
    fun `mainnet generates correct transaction URL`() {
        val provider = TONExternalLinkProvider(isTestnet = false)
        val txHash = "1234abcd"

        val txState = provider.getExplorerTxUrl(txHash)

        assertThat(txState).isInstanceOf(TxExploreState.Url::class.java)
        assertThat((txState as TxExploreState.Url).url).isEqualTo("https://tonviewer.com/transaction/$txHash")
    }

    @Test
    fun `testnet generates correct transaction URL`() {
        val provider = TONExternalLinkProvider(isTestnet = true)
        val txHash = "1234abcd"

        val txState = provider.getExplorerTxUrl(txHash)

        assertThat(txState).isInstanceOf(TxExploreState.Url::class.java)
        assertThat((txState as TxExploreState.Url).url).isEqualTo("https://testnet.tonviewer.com/transaction/$txHash")
    }
}