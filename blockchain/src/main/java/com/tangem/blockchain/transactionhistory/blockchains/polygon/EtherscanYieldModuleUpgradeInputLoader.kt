package com.tangem.blockchain.transactionhistory.blockchains.polygon

import com.tangem.Log
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.EtherScanApi
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.PolygonTransaction
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * `tokentx` reports no calldata ("deprecated"), and a call sent inside a yield module upgrade is typed by the
 * call it wraps, so the input of such a transaction is asked by its hash. Without it the call stays untyped.
 *
 * The selector is the generic UUPS `upgradeToAndCall`, so the loader runs only while yield module upgrades are
 * enabled. A loaded input never changes and is kept for the provider's lifetime, so a refresh does not ask again
 * and a rate-limited request does not turn an already typed transaction back into an untyped one.
 */
internal class EtherscanYieldModuleUpgradeInputLoader(
    private val blockchain: Blockchain,
    private val api: EtherScanApi,
    private val apiKey: String,
) {

    private val loadedInputs = ConcurrentHashMap<String, String>()

    private val isEnabled: Boolean
        get() = DepsContainer.blockchainFeatureToggles.isYieldModuleV3Enabled

    private val PolygonTransaction.isYieldModuleUpgradeWithoutInput: Boolean
        get() = methodId.equals(EthereumYieldSupplyUpgradeToAndCallCallData.METHOD_ID, ignoreCase = true) &&
            !input.startsWith(EthereumYieldSupplyUpgradeToAndCallCallData.METHOD_ID, ignoreCase = true)

    suspend fun withInputs(transactions: List<PolygonTransaction>): List<PolygonTransaction> {
        if (!isEnabled) return transactions

        val inputs = transactions.filter { it.isYieldModuleUpgradeWithoutInput }
            .mapTo(mutableSetOf(), PolygonTransaction::hash)
            .mapNotNull { hash -> getInput(hash)?.let { input -> hash to input } }
            .toMap()
        if (inputs.isEmpty()) return transactions

        return transactions.map { transaction ->
            inputs[transaction.hash]?.let { transaction.copy(input = it) } ?: transaction
        }
    }

    private suspend fun getInput(hash: String): String? =
        loadedInputs[hash] ?: loadInput(hash)?.also { loadedInputs[hash] = it }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadInput(hash: String): String? = try {
        withContext(Dispatchers.IO) {
            api.getTransactionByHash(
                chainId = requireNotNull(blockchain.getChainId()) { "chainId must not be null" },
                txHash = hash,
                apiKey = apiKey,
            ).result?.input
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.info { "Input of transaction $hash is not loaded: ${e.message}" }
        null
    }
}