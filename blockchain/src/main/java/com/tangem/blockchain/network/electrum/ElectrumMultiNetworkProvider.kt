package com.tangem.blockchain.network.electrum

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider

internal class ElectrumMultiNetworkProvider(providers: List<ElectrumNetworkProvider>) : ElectrumNetworkProvider {
    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    private val multiProvider = MultiNetworkProvider(providers)

    override suspend fun getAccount(addressScriptHash: String): Result<ElectrumAccount> =
        multiProvider.performRequest(ElectrumNetworkProvider::getAccount, addressScriptHash)

    override suspend fun getUnspentUTXOs(addressScriptHash: String): Result<List<ElectrumUnspentUTXORecord>> =
        multiProvider.performRequest(ElectrumNetworkProvider::getUnspentUTXOs, addressScriptHash)

    override suspend fun getEstimateFee(numberConfirmationBlocks: Int): Result<ElectrumEstimateFee> =
        multiProvider.performRequest(ElectrumNetworkProvider::getEstimateFee, numberConfirmationBlocks)

    override suspend fun getBlockTip(): Result<ElectrumBlockTip> =
        multiProvider.performRequest(ElectrumNetworkProvider::getBlockTip)

    override suspend fun sendTransaction(rawTransactionHash: String): Result<ElectrumTransactionHex> =
        multiProvider.performRequest(ElectrumNetworkProvider::sendTransaction, rawTransactionHash)
}