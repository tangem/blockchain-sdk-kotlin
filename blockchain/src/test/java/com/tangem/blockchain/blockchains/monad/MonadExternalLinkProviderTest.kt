package com.tangem.blockchain.blockchains.monad

import com.google.common.truth.Truth
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchain.externallinkprovider.providers.MonadExternalLinkProvider
import org.junit.Test

/**
 * Tests for MonadExternalLinkProvider
 */
internal class MonadExternalLinkProviderTest {

    @Test
    fun `MonadExternalLinkProvider mainnet has correct explorer URL`() {
        val provider = MonadExternalLinkProvider(isTestnet = false)

        Truth.assertThat(provider.explorerBaseUrl).isEqualTo("https://monadscan.com/")
    }

    @Test
    fun `MonadExternalLinkProvider testnet has correct explorer URL`() {
        val provider = MonadExternalLinkProvider(isTestnet = true)

        Truth.assertThat(provider.explorerBaseUrl).isEqualTo("https://testnet.monadscan.com/")
    }

    @Test
    fun `MonadExternalLinkProvider mainnet generates correct wallet URL`() {
        val provider = MonadExternalLinkProvider(isTestnet = false)
        val walletAddress = "0x1234567890abcdef1234567890abcdef12345678"

        val url = provider.explorerUrl(walletAddress, null)

        Truth.assertThat(url).isEqualTo("https://monadscan.com/address/$walletAddress")
    }

    @Test
    fun `MonadExternalLinkProvider testnet generates correct wallet URL`() {
        val provider = MonadExternalLinkProvider(isTestnet = true)
        val walletAddress = "0x1234567890abcdef1234567890abcdef12345678"

        val url = provider.explorerUrl(walletAddress, null)

        Truth.assertThat(url).isEqualTo("https://testnet.monadscan.com/address/$walletAddress")
    }

    @Test
    fun `MonadExternalLinkProvider mainnet generates correct transaction URL`() {
        val provider = MonadExternalLinkProvider(isTestnet = false)
        val txHash = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"

        val txState = provider.getExplorerTxUrl(txHash)

        Truth.assertThat(txState).isInstanceOf(TxExploreState.Url::class.java)
        val url = (txState as TxExploreState.Url).url
        Truth.assertThat(url).isEqualTo("https://monadscan.com/tx/$txHash")
    }

    @Test
    fun `MonadExternalLinkProvider testnet generates correct transaction URL`() {
        val provider = MonadExternalLinkProvider(isTestnet = true)
        val txHash = "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"

        val txState = provider.getExplorerTxUrl(txHash)

        Truth.assertThat(txState).isInstanceOf(TxExploreState.Url::class.java)
        val url = (txState as TxExploreState.Url).url
        Truth.assertThat(url).isEqualTo("https://testnet.monadscan.com/tx/$txHash")
    }

    @Test
    fun `MonadExternalLinkProvider has correct testnet faucet URL`() {
        val provider = MonadExternalLinkProvider(isTestnet = true)

        Truth.assertThat(provider.testNetTopUpUrl).isEqualTo("https://faucet.monad.xyz/")
    }

    @Test
    fun `MonadExternalLinkProvider ignores contract address in explorer URL`() {
        val provider = MonadExternalLinkProvider(isTestnet = false)
        val walletAddress = "0x1234567890abcdef1234567890abcdef12345678"
        val contractAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"

        val url = provider.explorerUrl(walletAddress, contractAddress)

        Truth.assertThat(url).isEqualTo("https://monadscan.com/address/$walletAddress")
        Truth.assertThat(url).doesNotContain(contractAddress)
    }
}