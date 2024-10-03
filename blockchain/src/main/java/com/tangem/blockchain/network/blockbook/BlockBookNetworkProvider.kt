package com.tangem.blockchain.network.blockbook

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.dash.DashMainNetParams
import com.tangem.blockchain.blockchains.ducatus.DucatusMainNetParams
import com.tangem.blockchain.blockchains.ravencoin.RavencoinMainNetParams
import com.tangem.blockchain.blockchains.ravencoin.RavencoinTestNetParams
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.toBigDecimalOrDefault
import com.tangem.blockchain.network.blockbook.config.BlockBookConfig
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.blockbook.network.responses.GetUtxoResponseItem
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Address
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.ScriptBuilder
import org.libdohj.params.DogecoinMainNetParams
import org.libdohj.params.LitecoinMainNetParams
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

class BlockBookNetworkProvider(
    val config: BlockBookConfig,
    val blockchain: Blockchain,
) : BitcoinNetworkProvider {

    override val baseUrl: String = config.baseHost

    private val api: BlockBookApi = BlockBookApi(config, blockchain)

    private var networkParameters = when (blockchain) {
        Blockchain.Bitcoin, Blockchain.BitcoinCash -> MainNetParams()
        Blockchain.BitcoinTestnet, Blockchain.BitcoinCashTestnet -> TestNet3Params()
        Blockchain.Litecoin -> LitecoinMainNetParams()
        Blockchain.Dogecoin -> DogecoinMainNetParams()
        Blockchain.Ducatus -> DucatusMainNetParams()
        Blockchain.Dash -> DashMainNetParams()
        Blockchain.Ravencoin -> RavencoinMainNetParams()
        Blockchain.RavencoinTestnet -> RavencoinTestNetParams()
        else -> error("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        return try {
            val getAddressResponse = withContext(Dispatchers.IO) { api.getAddress(address) }
            val getUtxoResponseItems = withContext(Dispatchers.IO) { api.getUtxo(address) }
            val balance = getAddressResponse.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO

            Result.Success(
                BitcoinAddressInfo(
                    balance = balance.movePointLeft(blockchain.decimals()),
                    unspentOutputs = createUnspentOutputs(
                        getUtxoResponseItems = getUtxoResponseItems,
                        address = address,
                    ),
                    recentTransactions = createRecentTransactions(
                        utxoResponseItems = getUtxoResponseItems,
                        address = address,
                    ),
                    hasUnconfirmed = getAddressResponse.unconfirmedTxs != 0,
                ),
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        return try {
            val minimalPerKb = getFeePerKb(param = MINIMAL_FEE_BLOCK_AMOUNT)
            val normalPerKb = getFeePerKb(param = NORMAL_FEE_BLOCK_AMOUNT)
            val priorityPerKb = getFeePerKb(param = PRIORITY_FEE_BLOCK_AMOUNT)

            Result.Success(
                BitcoinFee(minimalPerKb, normalPerKb, priorityPerKb),
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            val response = withContext(Dispatchers.IO) { api.sendTransaction(transaction) }

            if ((response.result as String).isNotBlank()) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(BlockchainSdkError.FailedToSendException)
            }
        } catch (e: Exception) {
            SimpleResult.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        return try {
            val response = withContext(Dispatchers.IO) { api.getAddress(address) }
            Result.Success(response.txs.plus(response.unconfirmedTxs ?: 0))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun createUnspentOutputs(
        getUtxoResponseItems: List<GetUtxoResponseItem>,
        address: String,
    ): List<BitcoinUnspentOutput> {
        val addressBitcoinJ = Address.fromString(networkParameters, address)
        val outputScript = ScriptBuilder.createOutputScript(addressBitcoinJ).program

        return getUtxoResponseItems.mapNotNull {
            val amount = it.value.toBigDecimalOrNull()?.movePointLeft(blockchain.decimals())
                ?: return@mapNotNull null

            BitcoinUnspentOutput(
                amount = amount,
                outputIndex = it.vout.toLong(),
                transactionHash = it.txid.hexToBytes(),
                outputScript = outputScript,
            )
        }
    }

    private suspend fun createRecentTransactions(
        utxoResponseItems: List<GetUtxoResponseItem>,
        address: String,
    ): List<BasicTransactionData> {
        return utxoResponseItems
            .filter { it.confirmations == 0 }
            .map { utxo ->
                val transaction = api.getTransaction(utxo.txid)
                val isIncoming = transaction.vin.any { it.addresses?.contains(address) == false }
                var source = "unknown"
                var destination = "unknown"
                val amount = if (isIncoming) {
                    destination = address
                    transaction.vin
                        .firstOrNull()
                        ?.addresses
                        ?.firstOrNull()
                        ?.let { source = it }
                    val outputs = transaction.vout
                        .find { it.addresses?.contains(address) == true }
                        ?.value.toBigDecimalOrDefault()
                    val inputs = transaction.vin
                        .find { it.addresses?.contains(address) == true }
                        ?.value.toBigDecimalOrDefault()
                    outputs - inputs
                } else {
                    source = address
                    transaction.vout
                        .firstOrNull()
                        ?.addresses
                        ?.firstOrNull()
                        ?.let { destination = it }
                    val outputs = transaction.vout
                        .asSequence()
                        .filter { it.addresses?.contains(address) == false }
                        .mapNotNull { it.value?.toBigDecimalOrNull() }
                        .sumOf { it }
                    val fee = transaction.fees.toBigDecimalOrDefault()
                    outputs + fee
                }.movePointLeft(blockchain.decimals())

                BasicTransactionData(
                    balanceDif = if (isIncoming) amount else amount.negate(),
                    hash = transaction.txid,
                    date = Calendar.getInstance().apply {
                        timeInMillis = transaction.blockTime.toLong()
                    },
                    isConfirmed = false,
                    destination = destination,
                    source = source,
                )
            }
    }

    private suspend fun getFeePerKb(param: Int): BigDecimal {
        val getFeeResponse = withContext(Dispatchers.IO) { api.getFee(param) }

        if (getFeeResponse.result.feerate <= 0) throw BlockchainSdkError.FailedToLoadFee

        return getFeeResponse.result.feerate
            .toBigDecimal()
            .setScale(blockchain.decimals(), RoundingMode.UP)
    }

    private companion object {
        const val MINIMAL_FEE_BLOCK_AMOUNT = 8
        const val NORMAL_FEE_BLOCK_AMOUNT = 4
        const val PRIORITY_FEE_BLOCK_AMOUNT = 1
    }
}