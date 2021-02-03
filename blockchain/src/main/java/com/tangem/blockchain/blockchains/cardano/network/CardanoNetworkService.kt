package com.tangem.blockchain.blockchains.cardano.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

interface CardanoNetworkService {
    suspend fun getInfo(addresses: Set<String>): Result<CardanoAddressResponse>
    suspend fun sendTransaction(transaction: String): SimpleResult
}