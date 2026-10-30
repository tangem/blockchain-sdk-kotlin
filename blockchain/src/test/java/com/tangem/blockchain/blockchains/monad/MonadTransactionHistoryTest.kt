package com.tangem.blockchain.blockchains.monad

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainFeatureToggles
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.transactionhistory.blockchains.polygon.EtherscanTransactionHistoryProvider
import org.junit.Test

internal class MonadTransactionHistoryTest {

    private val config = BlockchainSdkConfig(etherscanApiKey = "key")

    @Test
    fun `Monad uses etherscan transaction history provider while its toggle is on`() {
        initToggles(isMonadEnabled = true)

        val provider = TransactionHistoryProviderFactory.makeProvider(blockchain = Blockchain.Monad, config = config)

        assertThat(provider).isInstanceOf(EtherscanTransactionHistoryProvider::class.java)
    }

    @Test
    fun `Monad has no transaction history while its toggle is off`() {
        initToggles(isMonadEnabled = false)

        val provider = TransactionHistoryProviderFactory.makeProvider(blockchain = Blockchain.Monad, config = config)

        assertThat(provider).isSameInstanceAs(DefaultTransactionHistoryProvider)
    }

    @Test
    fun `MonadTestnet has no transaction history`() {
        initToggles(isMonadEnabled = true)

        val provider = TransactionHistoryProviderFactory.makeProvider(
            blockchain = Blockchain.MonadTestnet,
            config = config,
        )

        assertThat(provider).isSameInstanceAs(DefaultTransactionHistoryProvider)
    }

    private fun initToggles(isMonadEnabled: Boolean) {
        DepsContainer.onInit(
            config = config,
            featureToggles = BlockchainFeatureToggles(isYieldSupplyEnabled = true, isMonadEnabled = isMonadEnabled),
        )
    }
}