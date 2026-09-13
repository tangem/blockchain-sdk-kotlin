package com.tangem.blockchain.blockchains.electroneum

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.transactionhistory.blockchains.polygon.EtherscanTransactionHistoryProvider
import org.junit.Test

/**
 * Tests for Electroneum transaction history wiring
 */
internal class ElectroneumTransactionHistoryTest {

    @Test
    fun `Electroneum uses etherscan compatible transaction history provider`() {
        val provider = TransactionHistoryProviderFactory.makeProvider(
            blockchain = Blockchain.Electroneum,
            config = BlockchainSdkConfig(),
        )

        Truth.assertThat(provider).isInstanceOf(EtherscanTransactionHistoryProvider::class.java)
        Truth.assertThat(provider).isNotEqualTo(DefaultTransactionHistoryProvider)
    }

    @Test
    fun `ElectroneumTestnet uses etherscan compatible transaction history provider`() {
        val provider = TransactionHistoryProviderFactory.makeProvider(
            blockchain = Blockchain.ElectroneumTestnet,
            config = BlockchainSdkConfig(),
        )

        Truth.assertThat(provider).isInstanceOf(EtherscanTransactionHistoryProvider::class.java)
        Truth.assertThat(provider).isNotEqualTo(DefaultTransactionHistoryProvider)
    }
}