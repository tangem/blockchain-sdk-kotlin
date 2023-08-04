package com.tangem.blockchain.blockchains.kaspa.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import java.math.BigDecimal

interface KaspaNetworkProvider: NetworkProvider {
    suspend fun getInfo(address: String): Result<KaspaInfoResponse>
    suspend fun sendTransaction(transaction: KaspaTransactionBody): SimpleResult
}

data class KaspaInfoResponse(
    val balance: BigDecimal,
    val unspentOutputs: List<KaspaUnspentOutput>
)

class KaspaUnspentOutput(
    val amount: BigDecimal,
    val outputIndex: Long,
    val transactionHash: ByteArray,
    val outputScript: ByteArray
)