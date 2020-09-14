package com.tangem.blockchain.blockchains.bitcoin.network

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo.BitcoinfeesEarnApi
import com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo.BlockchainInfoApi
import com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo.BlockchainInfoProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherApi
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.*
import com.tangem.blockchain.network.blockcypher.BlockcypherProvider
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal
import java.util.*


class BitcoinNetworkManager(blockchain: Blockchain) : BitcoinProvider {

    private val blockcypherProvider by lazy {
        val api = createRetrofitInstance(API_BLOCKCYPHER)
                .create(BlockcypherApi::class.java)
        BlockcypherProvider(api, blockchain)
    }

    private val blockchainInfoProvider by lazy {
        val api = createRetrofitInstance(API_BLOCKCHAIN_INFO)
                .create(BlockchainInfoApi::class.java)
        val bitcoinfeesEarnApi = createRetrofitInstance(API_BITCOINFEES_EARN)
                .create(BitcoinfeesEarnApi::class.java)
        BlockchainInfoProvider(api, bitcoinfeesEarnApi)
    }

    private var provider: BitcoinProvider = blockchainInfoProvider

    private fun changeProvider() {
        provider = if (provider == blockchainInfoProvider) {
            blockcypherProvider
        } else {
            blockchainInfoProvider
        }
    }

    override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> {
        val result = provider.getInfo(address)
        when (result) {
            is Result.Success -> return result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return provider.getInfo(address)
                } else {
                    return result
                }
            }
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        val result = provider.getFee()
        when (result) {
            is Result.Success -> return result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return provider.getFee()
                } else {
                    return result
                }
            }
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        val result = provider.sendTransaction(transaction)
        when (result) {
            is SimpleResult.Success -> return result
            is SimpleResult.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return provider.sendTransaction(transaction)
                } else {
                    return result
                }
            }
        }
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        val result = provider.getSignatureCount(address)
        when (result) {
            is Result.Success -> return result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return provider.getSignatureCount(address)
                } else {
                    return result
                }
            }
        }
    }
}

data class BitcoinAddressInfo(
        val balance: BigDecimal,
        val unspentOutputs: List<BitcoinUnspentOutput>,
        val recentTransactions: List<BitcoinTransaction>
)

data class BitcoinFee(
        val minimalPerKb: BigDecimal,
        val normalPerKb: BigDecimal,
        val priorityPerKb: BigDecimal
)

data class BitcoinTransaction(
        val balanceDif: BigDecimal, //change of balance
        val hash: String,
        val date: Calendar?,
        val isConfirmed: Boolean
)