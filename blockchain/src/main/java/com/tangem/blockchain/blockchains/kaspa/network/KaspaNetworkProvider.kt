package com.tangem.blockchain.blockchains.kaspa.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

interface KaspaNetworkProvider : NetworkProvider {
    suspend fun getInfo(address: String): Result<KaspaInfoResponse>
    suspend fun sendTransaction(transaction: KaspaTransactionBody): Result<String?>
    suspend fun calculateFee(transactionData: KaspaTransactionData): Result<KaspaFeeEstimation>
}

data class KaspaInfoResponse(
    val balance: BigDecimal,
    val unspentOutputs: List<KaspaUnspentOutput>,
)

class KaspaUnspentOutput(
    val amount: BigDecimal,
    val outputIndex: Long,
    val transactionHash: ByteArray,
    val outputScript: ByteArray,
)

data class KaspaFeeEstimation(
    val mass: Long,
    val priorityBucket: KaspaFeeBucketResponse,
    val normalBuckets: List<KaspaFeeBucketResponse>,
    val lowBuckets: List<KaspaFeeBucketResponse>,
)