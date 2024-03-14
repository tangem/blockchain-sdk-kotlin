package com.tangem.blockchain.network.electrum

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result

interface ElectrumNetworkProvider : NetworkProvider {

    suspend fun getAccount(addressScriptHash: String): Result<ElectrumAccount>

    suspend fun getUnspentUTXOs(addressScriptHash: String): Result<List<ElectrumUnspentUTXORecord>>

    suspend fun getEstimateFee(numberConfirmationBlocks: Int): Result<ElectrumEstimateFee>
}