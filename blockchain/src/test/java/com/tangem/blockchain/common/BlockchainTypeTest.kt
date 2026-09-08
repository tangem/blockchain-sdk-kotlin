package com.tangem.blockchain.common

import com.google.common.truth.Truth
import org.junit.Test

internal class BlockchainTypeTest {

    @Test
    fun testBlockchainCannotBeBothEvmAndUtxo() {
        Blockchain.entries.forEach { blockchain ->
            val isEvm = blockchain.isEvm()
            val isUTXO = blockchain.isUTXO
            Truth.assertThat(isEvm && isUTXO).isEqualTo(false)
        }
    }

    /**
     * Robinhood Chain is an Ethereum L2 whose native coin is ETH. Being absent from the L2 list made the app
     * show it as a standalone coin instead of an ETH network.
     */
    @Test
    fun testRobinhoodIsL2EthereumNetwork() {
        Truth.assertThat(Blockchain.Robinhood.isL2EthereumNetwork()).isTrue()
    }

    /**
     * Katana is an Ethereum L2 (Polygon CDK / AggLayer) whose native coin is ETH, so it must be listed as an
     * L2 network rather than as a standalone coin.
     */
    @Test
    fun testKatanaIsL2EthereumNetwork() {
        Truth.assertThat(Blockchain.Katana.isL2EthereumNetwork()).isTrue()
    }

    /**
     * Guards the Katana chain ID against accidental edits: a wrong chain ID would make EIP-155 signatures
     * valid on another network.
     */
    @Test
    fun testKatanaChainId() {
        Truth.assertThat(Blockchain.Katana.getChainId()).isEqualTo(KATANA_CHAIN_ID)
    }

    /**
     * A mainnet whose native coin is ETH is an Ethereum L2 and vice versa. The app relies on this to show such a
     * network under the ETH coin instead of listing its own "<network>-ethereum" coin.
     */
    @Test
    fun testEthNativeMainnetsMatchL2EthereumNetworks() {
        val ethNativeMainnets = Blockchain.entries.filter {
            !it.isTestnet() && it.currency == Blockchain.Ethereum.currency && it != Blockchain.Ethereum
        }
        val l2Networks = Blockchain.entries.filter { !it.isTestnet() && it.isL2EthereumNetwork() }

        Truth.assertThat(l2Networks).containsExactlyElementsIn(ethNativeMainnets)
    }

    private companion object {
        const val KATANA_CHAIN_ID = 747474
    }
}