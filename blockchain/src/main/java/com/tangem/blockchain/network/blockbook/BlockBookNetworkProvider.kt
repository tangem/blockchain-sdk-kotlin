package com.tangem.blockchain.network.blockbook

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.UsedAddress
import com.tangem.blockchain.blockchains.bitcoin.network.XpubInfoResponse
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashAddressService
import com.tangem.blockchain.blockchains.clore.CloreMainNetParams
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
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetUtxoResponseItem
import com.tangem.blockchain.network.blockbook.network.responses.GetXpubResponse
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Address
import org.bitcoinj.core.LegacyAddress
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
        Blockchain.Clore -> CloreMainNetParams()
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

            if (response.result.isNotBlank()) {
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

    override suspend fun getInfoByXpub(xpub: String): Result<XpubInfoResponse> {
        return try {
            val (xpubResponse, xpubUtxoItems) = coroutineScope {
                val xpubResponseDeferred = async(Dispatchers.IO) { api.getXpubInfo(xpub) }
                val xpubUtxoDeferred = async(Dispatchers.IO) { api.getXpubUtxo(xpub) }
                xpubResponseDeferred.await() to xpubUtxoDeferred.await()
            }

            val ownAddresses = xpubResponse.tokens
                ?.map { it.name }
                ?.toSet()
                .orEmpty()

            val balance = xpubResponse.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO

            Result.Success(
                XpubInfoResponse(
                    balance = balance.movePointLeft(blockchain.decimals()),
                    unspentOutputs = createXpubUnspentOutputs(xpubUtxoItems),
                    usedAddresses = createUsedAddresses(xpubResponse),
                    hasUnconfirmed = xpubResponse.unconfirmedTxs ?: 0 != 0,
                    recentTransactions = createXpubRecentTransactions(
                        xpubResponse = xpubResponse,
                        ownAddresses = ownAddresses,
                    ),
                ),
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    override suspend fun getUtxoByXpub(xpub: String): Result<List<BitcoinUnspentOutput>> {
        return try {
            val xpubUtxoItems = withContext(Dispatchers.IO) { api.getXpubUtxo(xpub) }
            Result.Success(createXpubUnspentOutputs(xpubUtxoItems))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun createXpubUnspentOutputs(items: List<GetUtxoResponseItem>): List<BitcoinUnspentOutput> {
        val scriptCache = mutableMapOf<String, ByteArray>()
        return items.mapNotNull { item ->
            val amount = item.value.toBigDecimalOrNull()?.movePointLeft(blockchain.decimals())
                ?: return@mapNotNull null
            val address = item.address ?: return@mapNotNull null

            val outputScript = scriptCache.getOrPut(address) {
                ScriptBuilder.createOutputScript(resolveAddress(address)).program
            }

            BitcoinUnspentOutput(
                amount = amount,
                outputIndex = item.vout.toLong(),
                transactionHash = item.txid.hexToBytes(),
                outputScript = outputScript,
                address = address,
                derivationPath = item.path,
            )
        }
    }

    private fun createUsedAddresses(xpubResponse: GetXpubResponse): List<UsedAddress> {
        return xpubResponse.tokens?.map { token ->
            UsedAddress(
                address = token.name,
                path = token.path,
                balance = (token.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                    .movePointLeft(blockchain.decimals()),
            )
        }.orEmpty()
    }

    private fun createXpubRecentTransactions(
        xpubResponse: GetXpubResponse,
        ownAddresses: Set<String>,
    ): List<BasicTransactionData> {
        return xpubResponse.transactions
            ?.filter { it.confirmations == 0 }
            ?.map { tx ->
                val areAllVinOurs = tx.vin.all { vin ->
                    vin.isOwn == true || vin.addresses?.any { it in ownAddresses } == true
                }
                val isIncoming = !areAllVinOurs

                val source: String
                val destination: String
                val amount: BigDecimal

                if (isIncoming) {
                    source = tx.vin.firstOrNull()?.addresses?.firstOrNull() ?: "unknown"
                    val ownVouts = tx.vout.filter { vout -> isOwnVout(vout, ownAddresses) }
                    destination = ownVouts
                        .flatMap { it.addresses.orEmpty() }
                        .firstOrNull { it in ownAddresses }
                        ?: "unknown"
                    amount = ownVouts
                        .sumOf { it.value.toBigDecimalOrDefault() }
                        .movePointLeft(blockchain.decimals())
                } else {
                    source = ownAddresses.firstOrNull() ?: "unknown"
                    val externalVouts = tx.vout.filter { vout -> isExternalVout(vout, ownAddresses) }
                    destination = externalVouts.firstOrNull()?.addresses?.firstOrNull() ?: "unknown"
                    amount = externalVouts
                        .sumOf { it.value.toBigDecimalOrDefault() }
                        .movePointLeft(blockchain.decimals())
                }

                BasicTransactionData(
                    balanceDif = if (isIncoming) amount else amount.negate(),
                    hash = tx.txid,
                    date = Calendar.getInstance().apply {
                        timeInMillis = tx.blockTime.toLong() * MILLIS_IN_SECOND
                    },
                    isConfirmed = false,
                    destination = destination,
                    source = source,
                )
            }
            .orEmpty()
    }

    private fun isOwnVout(vout: GetAddressResponse.Transaction.Vout, ownAddresses: Set<String>): Boolean {
        if (vout.isOwn == true) return true
        return vout.addresses?.any { it in ownAddresses } == true
    }

    private fun isExternalVout(vout: GetAddressResponse.Transaction.Vout, ownAddresses: Set<String>): Boolean {
        return !isOwnVout(vout, ownAddresses)
    }

    private fun resolveAddress(address: String): Address {
        return when (blockchain) {
            Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> {
                val addressService = BitcoinCashAddressService(blockchain)
                LegacyAddress.fromPubKeyHash(networkParameters, addressService.getPublicKeyHash(address))
            }
            else -> Address.fromString(networkParameters, address)
        }
    }

    private fun createUnspentOutputs(
        getUtxoResponseItems: List<GetUtxoResponseItem>,
        address: String,
    ): List<BitcoinUnspentOutput> {
        val outputScript = ScriptBuilder.createOutputScript(resolveAddress(address)).program

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
        val feeRate = withContext(Dispatchers.IO) {
            when (blockchain) {
                // increased for clore, tx failed for some reason
                Blockchain.Clore -> api.getFees(param).result * CLORE_FEE_MULTIPLIER
                else -> api.getFee(param).result.feerate
            }
        }

        if (feeRate <= 0) throw BlockchainSdkError.FailedToLoadFee

        return feeRate
            .toBigDecimal()
            .setScale(blockchain.decimals(), RoundingMode.UP)
    }

    private companion object {
        const val MINIMAL_FEE_BLOCK_AMOUNT = 8
        const val NORMAL_FEE_BLOCK_AMOUNT = 4
        const val PRIORITY_FEE_BLOCK_AMOUNT = 1
        const val CLORE_FEE_MULTIPLIER = 1.5
        const val MILLIS_IN_SECOND = 1000L
    }
}