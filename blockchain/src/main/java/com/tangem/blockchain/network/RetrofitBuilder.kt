package com.tangem.blockchain.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

fun createRetrofitInstance(
    baseUrl: String,
    headerInterceptors: List<Interceptor> = emptyList(),
): Retrofit =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(BlockchainSdkRetrofitBuilder.build(headerInterceptors))
        .build()

object BlockchainSdkRetrofitBuilder {

    var interceptors: List<Interceptor> = emptyList()
    var timeoutConfig: TimeoutConfig? = null

    internal fun build(internalInterceptors: List<Interceptor> = emptyList()): OkHttpClient {
        val builder = OkHttpClient.Builder()
        (interceptors + internalInterceptors).forEach { builder.addInterceptor(it) }
        timeoutConfig?.let {
            builder.callTimeout(it.call.time, it.call.unit)
            builder.connectTimeout(it.connect.time, it.connect.unit)
            builder.readTimeout(it.read.time, it.read.unit)
            builder.writeTimeout(it.write.time, it.write.unit)
        }

        return builder.build()
    }
}

data class TimeoutConfig(
    val call: Timeout,
    val connect: Timeout,
    val read: Timeout,
    val write: Timeout,
) {
    companion object {
        fun default(): TimeoutConfig = TimeoutConfig(
            call = Timeout(10),
            connect = Timeout(20),
            read = Timeout(20),
            write = Timeout(20),
        )
    }
}

data class Timeout(
    val time: Long,
    val unit: TimeUnit = TimeUnit.SECONDS,
)

class BasicAuthInterceptor(
    private val username: String,
    private val password: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader("Authorization", Credentials.basic(username, password))
            .build()

        return chain.proceed(request)
    }
}

private val moshi: Moshi by lazy {
    Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
}

const val API_TANGEM = "https://verify.tangem.com/"
const val API_COINMARKETCAP = "https://pro-api.coinmarketcap.com/"
const val API_INFURA = "https://mainnet.infura.io/"
const val API_INFURA_TESTNET = "https://goerli.infura.io/"
const val API_ETH_CLASSIC_CLUSTER = "https://www.ethercluster.com/"
const val API_ETH_CLASSIC_BLOCKSCOUT = "https://blockscout.com/etc/mainnet/api/eth-rpc/"
const val API_ETH_CLASSIC_ETCDESKTOP = "https://etc.etcdesktop.com/"
const val API_ETH_CLASSIC_MYTOKEN = "https://etc.mytokenpocket.vip/"
const val API_ETH_CLASSIC_BESU = "https://besu.etc-network.info/"
const val API_ETH_CLASSIC_GETH = "https://geth.etc-network.info/"
const val API_SOCHAIN_V2 = "https://chain.so/"
const val API_UPDATE_VERSION = "https://raw.githubusercontent.com/"
const val API_RSK = "https://public-node.rsk.co/"
const val API_BLOCKCYPHER = "https://api.blockcypher.com/"
const val API_BINANCE = "https://dex.binance.org/"
const val API_BINANCE_TESTNET = "https://testnet-dex.binance.org/"
const val API_STELLAR = "https://horizon.stellar.org/"
const val API_STELLAR_RESERVE = "https://horizon.sui.li/"
const val API_STELLAR_TESTNET = "https://horizon-testnet.stellar.org/"
const val API_BLOCKCHAIN_INFO = "https://blockchain.info/"
const val API_ADALITE = "https://explorer2.adalite.io/"
const val API_XRP_LEDGER_FOUNDATION = "https://xrplcluster.com/"
const val API_RIPPLE = "https://s1.ripple.com:51234/"
const val API_RIPPLE_RESERVE = "https://s2.ripple.com:51234/"
const val API_BLOCKCHAIR = "https://api.blockchair.com/"
const val API_TEZOS_LETZBAKE = "https://teznode.letzbake.com/"
const val API_TEZOS_BLOCKSCALE = "https://rpc.tzbeta.net/"
const val API_TEZOS_SMARTPY = "https://mainnet.smartpy.io/"
const val API_TEZOS_ECAD = "https://api.tez.ie/rpc/mainnet/"
const val API_DUCATUS = "https://ducapi.rocknblock.io/"
const val API_BITCOINFEES_EARN = "https://bitcoinfees.earn.com/"
const val API_TANGEM_ROSETTA = "https://ada.tangem.com/"
const val API_BSC = "https://bsc-dataseed.binance.org/"
const val API_BSC_TESTNET = "https://data-seed-prebsc-1-s1.binance.org:8545/"
const val API_POLYGON = "https://polygon-rpc.com/"
const val API_POLYGON_MATICVIGIL = "https://rpc-mainnet.maticvigil.com/"
const val API_POLYGON_TESTNET = "https://rpc-mumbai.maticvigil.com/"
const val API_BLOCKCKAIR_TANGEM = "https://api.tangem-tech.com/"
const val API_AVALANCHE = "https://api.avax.network/"
const val API_AVALANCHE_TESTNET = "https://api.avax-test.network/"
const val API_FANTOM_TOOLS = "https://rpc.ftm.tools/"
const val API_FANTOM_NETWORK = "https://rpcapi.fantom.network/"
const val API_FANTOM_ANKR_TOOLS = "http://rpc.ankr.tools/"                      // ftm
const val API_FANTOM_ULTIMATENODES = "https://ftmrpc.ultimatenodes.io/"
const val API_FANTOM_TESTNET = "https://rpc.testnet.fantom.network/"
const val API_ARBITRUM = "https://arb1.arbitrum.io/"
const val API_ARBITRUM_INFURA = "https://arbitrum-mainnet.infura.io/v3/"
const val API_ARBITRUM_OFFCHAIN = "https://node.offchainlabs.com:8547/"
const val API_ARBITRUM_TESTNET = "https://goerli-rollup.arbitrum.io/"
const val API_GNOSIS_CHAIN = "https://rpc.gnosischain.com/"
const val API_GNOSIS_POKT = "https://gnosischain-rpc.gateway.pokt.network/"
const val API_GNOSIS_ANKR = "https://rpc.ankr.com/gnosis/"
const val API_GNOSIS_BLAST = "https://gnosis-mainnet.public.blastapi.io/"
const val API_XDAI_POKT = "https://xdai-rpc.gateway.pokt.network/"
const val API_XDAI_BLOCKSCOUT = "https://xdai-archive.blockscout.com/"
const val API_ETH_FAIR_RPC = "https://rpc.etherfair.org"
const val API_ETH_POW_RPC = "https://mainnet.ethereumpow.org"
const val API_ETH_POW_TESTNET_RPC = "https://iceberg.ethereumpow.org"
const val API_OPTIMISM = "https://mainnet.optimism.io/"
const val API_OPTIMISM_BLAST = "https://optimism-mainnet.public.blastapi.io/"
const val API_OPTIMISM_ANKR = "https://rpc.ankr.com/"
const val API_OPTIMISM_TESTNET = "https://goerli.optimism.io/"
const val API_SALTPAY = "https://rpc.bicoccachain.net/"
