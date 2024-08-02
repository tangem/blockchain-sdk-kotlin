package com.tangem.blockchain.blockchains.filecoin.network

import com.tangem.blockchain.blockchains.filecoin.models.FilecoinAccountInfo
import com.tangem.blockchain.blockchains.filecoin.models.FilecoinTxGasInfo
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

    /** Estimate gas [FilecoinTxGasInfo] to send transaction [transactionInfo] */
    suspend fun estimateMessageGas(transactionInfo: FilecoinTxInfo): Result<FilecoinTxGasInfo>

    /** Submit transaction [signedTransactionBody] */
    suspend fun submitTransaction(signedTransactionBody: FilecoinSignedTransactionBody): Result<String>
}