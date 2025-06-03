package com.tangem.blockchain.blockchains.pepecoin.network

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.pepecoin.PepecoinMainNetParams
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.*
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.extensions.toSimpleFailure
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import com.tangem.blockchain.network.electrum.ElectrumUnspentUTXORecord
import com.tangem.blockchain.network.electrum.api.ElectrumResponse
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.isZero
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import java.math.BigDecimal
import java.util.Calendar

internal class PepecoinNetworkService(
    private val blockchain: Blockchain,
    providers: List<ElectrumNetworkProvider>,
) : BitcoinNetworkProvider {

    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    private val multiProvider = MultiNetworkProvider(providers)

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> = coroutineScope {
        val scriptHash = generateAddressScriptHash(address)
        val balanceDeferred =
            async { multiProvider.performRequest(ElectrumNetworkProvider::getAccountBalance, scriptHash) }
        val unspentsDeferred =
            async { multiProvider.performRequest(ElectrumNetworkProvider::getUnspentUTXOs, scriptHash) }
        val balance = balanceDeferred.await().successOr { return@coroutineScope it }
        val unspentsUTXOs = unspentsDeferred.await().successOr { return@coroutineScope it }

        val info = BitcoinAddressInfo(
            balance = balance.confirmedAmount,
            unspentOutputs = createUnspentOutputs(
                getUtxoResponseItems = unspentsUTXOs,
                address = address,
            ),
            recentTransactions = createRecentTransactions(
                utxoResponseItems = unspentsUTXOs,
                walletAddress = address,
            ),
            hasUnconfirmed = !balance.unconfirmedAmount.isZero(),
        )
        Result.Success(info)
    }

    override suspend fun getFee(): Result<BitcoinFee> = coroutineScope {
        Result.Success(
            BitcoinFee(
                minimalPerKb = MINIMAL_FEE_PER_KB,
                normalPerKb = NORMAL_FEE_PER_KB,
                priorityPerKb = PRIORITY_FEE_PER_KB,
            ),
        )
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return multiProvider.performRequest(ElectrumNetworkProvider::broadcastTransaction, transaction.hexToBytes())
            .map { SimpleResult.Success }
            .successOr { it.toSimpleFailure() }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return multiProvider.performRequest(
            ElectrumNetworkProvider::getTransactionHistory,
            generateAddressScriptHash(address),
        )
            .map { Result.Success(it.count()) }
            .successOr { it }
    }

    private fun createUnspentOutputs(
        getUtxoResponseItems: List<ElectrumUnspentUTXORecord>,
        address: String,
    ): List<BitcoinUnspentOutput> = getUtxoResponseItems.map {
        val amount = it.value
        BitcoinUnspentOutput(
            amount = amount,
            outputIndex = it.txPos,
            transactionHash = it.txHash.hexToBytes(),
            outputScript = addressToScript(address).program,
        )
    }

    private suspend fun createRecentTransactions(
        utxoResponseItems: List<ElectrumUnspentUTXORecord>,
        walletAddress: String,
    ): List<BasicTransactionData> = coroutineScope {
        utxoResponseItems
            .filter { !it.isConfirmed }
            .map { utxo -> async { multiProvider.performRequest { getTransactionInfo(utxo.txHash) } } }
            .awaitAll()
            .filterIsInstance<Result.Success<ElectrumResponse.Transaction>>()
            .map { result ->
                val transaction: ElectrumResponse.Transaction = result.data
                val vout = transaction.vout ?: listOf()
                val isIncoming = vout.any {
                    val publicKey = it.scriptPublicKey?.addresses?.firstOrNull()
                    publicKey == walletAddress
                }
                val otherAddress = vout
                    .firstOrNull { output ->
                        val outputAddress = output.scriptPublicKey?.addresses?.firstOrNull() ?: return@firstOrNull false
                        outputAddress != walletAddress
                    }
                    ?.scriptPublicKey
                    ?.addresses?.firstOrNull() ?: "unknown"
                val balanceDiff = if (isIncoming) {
                    val outputs = vout.filter { output ->
                        val outputAddress = output.scriptPublicKey?.addresses?.firstOrNull() ?: return@filter false
                        outputAddress == walletAddress
                    }
                    outputs
                        .map { it.value.toBigDecimal() }
                        .fold(BigDecimal.ZERO) { acc, bigDecimal -> acc + bigDecimal }
                } else {
                    val fee = transaction.fee?.toBigDecimal() ?: BigDecimal.ZERO
                    val outputs = vout.filter { output ->
                        val outputAddress = output.scriptPublicKey?.addresses?.firstOrNull() ?: return@filter false
                        outputAddress != walletAddress
                    }
                    outputs
                        .map { it.value.toBigDecimal() }
                        .fold(BigDecimal.ZERO) { acc, bigDecimal -> acc + bigDecimal }
                        .plus(fee)
                        .negate()
                }

                BasicTransactionData(
                    balanceDif = balanceDiff,
                    hash = transaction.txid,
                    date = Calendar.getInstance(),
                    isConfirmed = false,
                    destination = if (isIncoming) walletAddress else otherAddress,
                    source = if (isIncoming) otherAddress else walletAddress,
                )
            }
    }

    companion object {
        const val SUPPORTED_SERVER_VERSION = "1.4"
        private val MINIMAL_FEE_PER_KB = BigDecimal("0.01")
        private val NORMAL_FEE_PER_KB = MINIMAL_FEE_PER_KB * BigDecimal("10")
        private val PRIORITY_FEE_PER_KB = MINIMAL_FEE_PER_KB * BigDecimal("100")

        private fun addressToScript(walletAddress: String): Script {
            val address = LegacyAddress.fromBase58(
                PepecoinMainNetParams(),
                walletAddress,
            )
            return ScriptBuilder.createOutputScript(address)
        }

        private fun generateAddressScriptHash(walletAddress: String): String {
            val p2pkhScript = addressToScript(walletAddress)
            val sha256Hash = Sha256Hash.hash(p2pkhScript.program)
            return sha256Hash.reversedArray().toHexString()
        }
    }
}