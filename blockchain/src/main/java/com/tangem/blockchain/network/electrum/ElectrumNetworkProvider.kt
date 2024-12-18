package com.tangem.blockchain.network.electrum

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.electrum.api.ElectrumResponse

internal interface ElectrumNetworkProvider : NetworkProvider {

    suspend fun getAccountBalance(addressScriptHash: String): Result<ElectrumAccount>

    suspend fun getUnspentUTXOs(addressScriptHash: String): Result<List<ElectrumUnspentUTXORecord>>

    suspend fun getEstimateFee(numberConfirmationBlocks: Int): Result<ElectrumEstimateFee>

    suspend fun getTransactionInfo(txHash: String): Result<ElectrumResponse.Transaction>

    suspend fun broadcastTransaction(rawTx: ByteArray): Result<ElectrumResponse.TxHex>

    suspend fun getTransactionHistory(addressScriptHash: String): Result<List<ElectrumResponse.TxHistoryEntry>>
}