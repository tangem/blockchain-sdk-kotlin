package com.tangem.blockchain.blockchains.cardano.network

import com.tangem.blockchain.blockchains.cardano.network.common.models.CardanoAddressResponse
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

internal interface CardanoNetworkProvider : NetworkProvider {

    suspend fun getInfo(input: InfoInput): Result<CardanoAddressResponse>

    suspend fun sendTransaction(transaction: ByteArray): SimpleResult
}

data class InfoInput(val addresses: Set<String>, val tokens: Set<Token>)