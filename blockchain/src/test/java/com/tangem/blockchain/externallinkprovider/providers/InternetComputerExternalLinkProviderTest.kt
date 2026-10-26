package com.tangem.blockchain.externallinkprovider.providers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.externallinkprovider.TxExploreState
import org.junit.Test

internal class InternetComputerExternalLinkProviderTest {

    private val provider = InternetComputerExternalLinkProvider()

    @Test
    fun `has correct explorer base URL`() {
        assertThat(provider.explorerBaseUrl).isEqualTo("https://dashboard.internetcomputer.org/")
    }

    @Test
    fun `generates correct wallet URL`() {
        val walletAddress = "4e21a73d443cc0c11e6544a8aace1364ef465815de1183b632ffb1f7262482d2"

        val url = provider.explorerUrl(walletAddress, contractAddress = null)

        assertThat(url).isEqualTo("https://dashboard.internetcomputer.org/tokens/account/$walletAddress")
    }

    @Test
    fun `generates correct transaction URL`() {
        val txHash = "e5339a0c4aa822b699f8f70d31ff9d2793a5d0b338d45001ec2cc421fb3503a0"

        val txState = provider.getExplorerTxUrl(txHash)

        assertThat(txState).isInstanceOf(TxExploreState.Url::class.java)
        assertThat((txState as TxExploreState.Url).url)
            .isEqualTo("https://dashboard.internetcomputer.org/tokens/tx-hash/$txHash")
    }
}