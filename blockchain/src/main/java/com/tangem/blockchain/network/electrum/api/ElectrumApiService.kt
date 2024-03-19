package com.tangem.blockchain.network.electrum.api

import com.tangem.blockchain.extensions.Result

/**
 * Service to communicate with Electrum server
 */
internal interface ElectrumApiService {

    /**
     * The client should send a server.version RPC call as early as possible
     * in order to negotiate the precise protocol version;
     * see its description for more detail.
     * All responses received in the stream from and including the server's response
     * to this call will use its negotiated protocol version.
     */
    suspend fun getServerVersion(
        clientName: String = "Tangem Android",
        supportedProtocolVersion: String = SUPPORTED_PROTOCOL_VERSION,
    ): Result<ElectrumResponse.ServerInfo>

    suspend fun getBlockTip(): Result<ElectrumResponse.BlockTip>

    suspend fun getBalance(addressScriptHash: String): Result<ElectrumResponse.Balance>

    suspend fun getTransactionHistory(addressScriptHash: String): Result<List<ElectrumResponse.TxHistoryEntry>>

    suspend fun getTransaction(txHash: String): Result<ElectrumResponse.Transaction>

    suspend fun sendTransaction(rawTransactionHex: String): Result<ElectrumResponse.TxHex>

    suspend fun getUnspentUTXOs(addressScriptHash: String): Result<List<ElectrumResponse.UnspentUTXORecord>>

    suspend fun getEstimateFee(numberConfirmationBlocks: Int): Result<ElectrumResponse.EstimateFee>

    companion object {
        const val SUPPORTED_PROTOCOL_VERSION = "1.4.3"
    }
}