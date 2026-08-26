package com.tangem.blockchain.blockchains.igra

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.transactionhistory.blockchains.polygon.EtherscanTransactionHistoryProvider
import org.junit.Test

/**
 * Tests for Igra transaction history wiring
 */
internal class IgraTransactionHistoryTest {

    @Test
    fun `Igra uses etherscan compatible transaction history provider`() {
        val provider = TransactionHistoryProviderFactory.makeProvider(
            blockchain = Blockchain.Igra,
            config = BlockchainSdkConfig(),
        )

        Truth.assertThat(provider).isInstanceOf(EtherscanTransactionHistoryProvider::class.java)
    }

    @Test
    fun `IgraTestnet uses etherscan compatible transaction history provider`() {
        val provider = TransactionHistoryProviderFactory.makeProvider(
            blockchain = Blockchain.IgraTestnet,
            config = BlockchainSdkConfig(),
        )

        Truth.assertThat(provider).isInstanceOf(EtherscanTransactionHistoryProvider::class.java)
        Truth.assertThat(provider).isNotEqualTo(DefaultTransactionHistoryProvider)
    }
}