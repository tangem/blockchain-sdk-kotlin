package com.tangem.blockchain.blockchains.onyx

import com.google.common.truth.Truth
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchain.externallinkprovider.providers.OnyxExternalLinkProvider
import org.junit.Test

internal class OnyxExternalLinkProviderTest {

    @Test
    fun `OnyxExternalLinkProvider mainnet generates wallet and transaction URLs`() {
        val provider = OnyxExternalLinkProvider(isTestnet = false)

        Truth.assertThat(provider.explorerUrl(walletAddress = WALLET_ADDRESS, contractAddress = null))
            .isEqualTo("https://explorer.onyx.org/address/$WALLET_ADDRESS")
        Truth.assertThat(provider.getExplorerTxUrl(TX_HASH))
            .isEqualTo(TxExploreState.Url("https://explorer.onyx.org/tx/$TX_HASH"))
    }

    @Test
    fun `OnyxExternalLinkProvider testnet generates wallet and transaction URLs`() {
        val provider = OnyxExternalLinkProvider(isTestnet = true)

        Truth.assertThat(provider.explorerUrl(walletAddress = WALLET_ADDRESS, contractAddress = null))
            .isEqualTo("https://testnet.explorer.goliath.net/address/$WALLET_ADDRESS")
        Truth.assertThat(provider.getExplorerTxUrl(TX_HASH))
            .isEqualTo(TxExploreState.Url("https://testnet.explorer.goliath.net/tx/$TX_HASH"))
    }

    private companion object {
        const val WALLET_ADDRESS = "0x3BbED2aAdBD84FAd82265E07d786F2184a671cb8"
        const val TX_HASH = "0x6e40fc9d80b51dc32d2054fd97efa50fa38fff773d0533b61680c120a898b498"
    }
}