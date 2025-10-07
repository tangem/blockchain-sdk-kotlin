package com.tangem.blockchain.network.electrum

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.electrum.api.ElectrumResponse

internal class ElectrumNetworkService(
    providers: List<ElectrumNetworkProvider>,
    blockchain: Blockchain,
) : ElectrumNetworkProvider {
    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    private val multiProvider = MultiNetworkProvider(providers, blockchain)

    override suspend fun getAccountBalance(addressScriptHash: String): Result<ElectrumAccount> =
        multiProvider.performRequest(ElectrumNetworkProvider::getAccountBalance, addressScriptHash)

    override suspend fun getEstimateFee(numberConfirmationBlocks: Int): Result<ElectrumEstimateFee> =
        multiProvider.performRequest(ElectrumNetworkProvider::getEstimateFee, numberConfirmationBlocks)

    override suspend fun getUnspentUTXOs(addressScriptHash: String): Result<List<ElectrumUnspentUTXORecord>> =
        multiProvider.performRequest(ElectrumNetworkProvider::getUnspentUTXOs, addressScriptHash)

    override suspend fun getTransactionInfo(txHash: String): Result<ElectrumResponse.Transaction> =
        multiProvider.performRequest(ElectrumNetworkProvider::getTransactionInfo, txHash)

    override suspend fun broadcastTransaction(rawTx: ByteArray): Result<ElectrumResponse.TxHex> =
        multiProvider.performRequest(ElectrumNetworkProvider::broadcastTransaction, rawTx)

    override suspend fun getTransactionHistory(
        addressScriptHash: String,
    ): Result<List<ElectrumResponse.TxHistoryEntry>> =
        multiProvider.performRequest(ElectrumNetworkProvider::getTransactionHistory, addressScriptHash)
}