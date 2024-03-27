package com.tangem.blockchain.blockchains.cardano.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

interface CardanoNetworkProvider : NetworkProvider {

    suspend fun getInfo(input: InfoInput): Result<CardanoAddressResponse>

    suspend fun sendTransaction(transaction: ByteArray): SimpleResult
}

data class InfoInput(val addresses: Set<String>, val tokens: Set<Token>)

data class CardanoAddressResponse(
    val balance: Long,
    val tokenBalances: Map<Token, Long> = emptyMap(),
    val unspentOutputs: List<CardanoUnspentOutput>,
    val recentTransactionsHashes: List<String>,
)

class CardanoUnspentOutput(
    val address: String,
    val amount: Long,
    val outputIndex: Long,
    val transactionHash: ByteArray,
    val assets: List<Asset> = emptyList(),
) {

    data class Asset(
        val policyID: String,
        val assetNameHex: String,
        val amount: Long,
    )
}