package com.tangem.blockchain.blockchains.publicmasterpiece

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.externallinkprovider.TxExploreState
import org.junit.Test

internal class PublicMasterpieceExternalLinkProviderTest {

    private val blockchain = Blockchain.PublicMasterpiece

    @Test
    fun `Public Masterpiece generates correct wallet URL`() {
        val walletAddress = "0xd8185212c609a99c3446E7044D605C6732bdae7a"

        val url = blockchain.getExploreUrl(walletAddress)

        Truth.assertThat(url).isEqualTo("https://pmtscan.com/address/$walletAddress")
    }

    @Test
    fun `Public Masterpiece generates correct transaction URL`() {
        val txHash = "0xd4ead4f0b2c4761f15815cbcf5e885e8a4f9c0985346edc411d66fa49b4fbd75"

        val txState = blockchain.getExploreTxUrl(txHash)

        Truth.assertThat(txState).isEqualTo(TxExploreState.Url("https://pmtscan.com/tx/$txHash"))
    }

    @Test
    fun `Public Masterpiece has no testnet faucet`() {
        Truth.assertThat(blockchain.getTestnetTopUpUrl()).isNull()
    }
}