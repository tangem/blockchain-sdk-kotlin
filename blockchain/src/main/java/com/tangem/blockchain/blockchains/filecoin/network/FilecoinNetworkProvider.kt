package com.tangem.blockchain.blockchains.filecoin.network

import com.tangem.blockchain.blockchains.filecoin.models.FilecoinAccountInfo
import com.tangem.blockchain.blockchains.filecoin.models.FilecoinTxInfo
import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinSignedTransactionBody
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result

/**
 * Filecoin network provider
 *
[REDACTED_AUTHOR]
 */
internal interface FilecoinNetworkProvider : NetworkProvider {

    /** Get account information by [address] */
    suspend fun getAccountInfo(address: String): Result<FilecoinAccountInfo>

    /** Estimate gas unit price to send transaction [transactionInfo] */
    suspend fun estimateGasUnitPrice(transactionInfo: FilecoinTxInfo): Result<Long>

    /** Estimate required gas units amount to send transaction [transactionInfo] */
    suspend fun estimateGasLimit(transactionInfo: FilecoinTxInfo): Result<Long>

    /** Submit transaction [signedTransactionBody] */
    suspend fun submitTransaction(signedTransactionBody: FilecoinSignedTransactionBody): Result<String>
}