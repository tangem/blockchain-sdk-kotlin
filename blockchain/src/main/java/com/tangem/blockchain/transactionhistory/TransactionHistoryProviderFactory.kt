package com.tangem.blockchain.transactionhistory

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider

internal interface TransactionHistoryProviderFactory {

    fun makeProvider(config: BlockchainSdkConfig, blockchain: Blockchain): TransactionHistoryProvider?
}