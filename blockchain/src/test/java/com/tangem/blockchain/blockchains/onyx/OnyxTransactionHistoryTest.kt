package com.tangem.blockchain.blockchains.onyx

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory
import com.tangem.blockchain.transactionhistory.blockchains.polygon.EtherscanTransactionHistoryProvider
import org.junit.Test

internal class OnyxTransactionHistoryTest {

    @Test
    fun `Onyx uses etherscan compatible transaction history provider`() {
        val provider = TransactionHistoryProviderFactory.makeProvider(
            blockchain = Blockchain.Onyx,
            config = BlockchainSdkConfig(),
        )

        Truth.assertThat(provider).isInstanceOf(EtherscanTransactionHistoryProvider::class.java)
    }

    @Test
    fun `OnyxTestnet uses etherscan compatible transaction history provider`() {
        val provider = TransactionHistoryProviderFactory.makeProvider(
            blockchain = Blockchain.OnyxTestnet,
            config = BlockchainSdkConfig(),
        )

        Truth.assertThat(provider).isInstanceOf(EtherscanTransactionHistoryProvider::class.java)
    }
}