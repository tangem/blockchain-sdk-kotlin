package com.tangem.blockchain.blockchains.near.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result

/**
[REDACTED_AUTHOR]
 */
interface NearNetworkProvider: NetworkProvider {

    suspend fun getAccount(address: String): Result<ViewAccountResult>

    suspend fun getGas(blockHeight: Long): Result<GasPriceResult>

    suspend fun getTransactionStatus(txHash: String, senderAccountId: String): Result<TransactionStatusResult>

    suspend fun sendTransaction(signedTxBase64: String): Result<SendTransactionAsyncResult>
}