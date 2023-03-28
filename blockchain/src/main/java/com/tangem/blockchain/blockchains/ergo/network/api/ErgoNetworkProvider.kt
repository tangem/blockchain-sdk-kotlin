package com.tangem.blockchain.blockchains.ergo.network.api

import com.tangem.blockchain.blockchains.ergo.network.ErgoAddressResponse
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoAddressRequestData
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiBlockResponse
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiSendTransactionResponse
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiUnspentResponse
import com.tangem.blockchain.extensions.Result

interface ErgoNetworkProvider {
    val host: String
    //https://ergo-explorer.getblok.io/api/v0
    suspend fun getInfo(data: ErgoAddressRequestData): Result<ErgoAddressResponse>

    suspend fun getLastBlock(): Result<ErgoApiBlockResponse>

    suspend fun getUnspent(address: String): Result<List<ErgoApiUnspentResponse>>

    suspend fun sendTransaction(transaction: String): Result<ErgoApiSendTransactionResponse>
}