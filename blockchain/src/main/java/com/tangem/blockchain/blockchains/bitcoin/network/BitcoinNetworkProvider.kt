package com.tangem.blockchain.blockchains.bitcoin.network

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import java.math.BigDecimal

interface BitcoinNetworkProvider : NetworkProvider {
    suspend fun getInfo(address: String): Result<BitcoinAddressInfo>
    suspend fun getFee(): Result<BitcoinFee>
    suspend fun sendTransaction(transaction: String): SimpleResult
    suspend fun getSignatureCount(address: String): Result<Int>

    suspend fun getInfoByXpub(xpub: String): Result<XpubInfoResponse> {
        return Result.Failure(BlockchainSdkError.CustomError("Not implemented yet"))
    }

    suspend fun getUtxoByXpub(xpub: String): Result<List<BitcoinUnspentOutput>> {
        return Result.Failure(BlockchainSdkError.CustomError("Not implemented yet"))
    }
}

data class BitcoinAddressInfo(
    val balance: BigDecimal,
    val unspentOutputs: List<BitcoinUnspentOutput>,
    val recentTransactions: List<BasicTransactionData>,
    val hasUnconfirmed: Boolean? = null, // additional logic for when recent transactions are absent or not reliable
)

data class BitcoinFee(
    val minimalPerKb: BigDecimal,
    val normalPerKb: BigDecimal,
    val priorityPerKb: BigDecimal,
)