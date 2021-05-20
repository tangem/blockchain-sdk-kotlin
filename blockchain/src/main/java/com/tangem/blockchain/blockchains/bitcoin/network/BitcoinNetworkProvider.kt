package com.tangem.blockchain.blockchains.bitcoin.network

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import java.math.BigDecimal
import java.util.*

interface BitcoinNetworkProvider {
    val supportsRbf: Boolean
    suspend fun getInfo(address: String): Result<BitcoinAddressInfo>
    suspend fun getFee(): Result<BitcoinFee>
    suspend fun sendTransaction(transaction: String): SimpleResult
    suspend fun getTransaction(transactionHash: String): Result<BitcoinTransaction>
    suspend fun getSignatureCount(address: String): Result<Int>
}

data class BitcoinAddressInfo(
        val balance: BigDecimal,
        val unspentOutputs: List<BitcoinUnspentOutput>,
        val recentTransactions: List<BasicTransactionData>,
        val hasUnconfirmed: Boolean? = null //additional logic for when recent transactions are absent or not reliable
)

data class BitcoinFee(
        val minimalPerKb: BigDecimal,
        val normalPerKb: BigDecimal,
        val priorityPerKb: BigDecimal
)

data class BitcoinTransaction(
        val hash: String,
        val isConfirmed: Boolean,
        val time: Calendar,
        val inputs: List<BitcoinTransactionInput>,
        val outputs: List<BitcoinTransactionOutput>
)

data class BitcoinTransactionInput(
        val unspentOutput: BitcoinUnspentOutput,
        val sender: String,
        val sequence: Long
)

data class BitcoinTransactionOutput(
        val amount: BigDecimal,
        val recipient: String
)