package com.tangem.blockchain.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
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

private val moshi: Moshi by lazy {
    Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
}

const val API_TANGEM = "https://verify.tangem.com/"
const val API_COINMARKETCAP = "https://pro-api.coinmarketcap.com/"
const val API_SOCHAIN_V2 = "https://chain.so/"
const val API_UPDATE_VERSION = "https://raw.githubusercontent.com/"
const val API_BLOCKCYPHER = "https://api.blockcypher.com/"
const val API_BLOCKCKAIR_TANGEM = "https://api.tangem-tech.com/"
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

//region ETHEREUM API
const val API_ETH = "https://mainnet.infura.io/v3/"
const val API_ETH_NOWNODES = "https://eth.nownodes.io/"
const val API_ETH_GETBLOCK = "https://eth.getblock.io/mainnet/"
const val API_ETH_TESTNET = "https://goerli.infura.io/v3/"
const val API_ETH_NOWNODES_TESTNET = "https://eth-goerli.nownodes.io/"
//endregion

//region ETHEREUM CLASSIC API
const val API_ETH_CLASSIC_CLUSTER = "https://www.ethercluster.com/etc/"
const val API_ETH_CLASSIC_BLOCKSCOUT = "https://blockscout.com/etc/mainnet/api/eth-rpc/"
const val API_ETH_CLASSIC_ETCDESKTOP = "https://etc.etcdesktop.com/"
const val API_ETH_CLASSIC_MYTOKEN = "https://etc.mytokenpocket.vip/"
const val API_ETH_CLASSIC_BESU = "https://besu.etc-network.info/"
const val API_ETH_CLASSIC_GETH = "https://geth.etc-network.info/"
const val API_ETH_CLASSIC_GETBLOCK = "https://etc.getblock.io/mainnet/"
const val API_ETH_CLASSIC_CLUSTER_TESTNET = "https://www.ethercluster.com/kotti/"
//endregion

//region RSK API
const val API_RSK = "https://public-node.rsk.co/"
const val API_RSK_GETBLOCK = "https://rsk.getblock.io/mainnet/"
//endregion

//region BSC API
const val API_BSC = "https://bsc-dataseed.binance.org/"
const val API_BSC_NOWNODES = "https://bsc.nownodes.io/"
const val API_BSC_GETBLOCK = "https://bsc.getblock.io/mainnet/"
const val API_BSC_TESTNET = "https://data-seed-prebsc-1-s1.binance.org:8545/"
//endregion

//region POLYGON API
const val API_POLYGON = "https://polygon-rpc.com/"
const val API_POLYGON_MATICVIGIL = "https://rpc-mainnet.maticvigil.com/"
const val API_POLYGON_NOWNODES = "https://matic.nownodes.io/"
const val API_POLYGON_GETBLOCK = "https://matic.getblock.io/mainnet/"
const val API_POLYGON_MATIC = "https://rpc-mainnet.matic.network/"
const val API_POLYGON_CHAINSTACKLABS = "https://matic-mainnet.chainstacklabs.com/"
const val API_POLYGON_QUICKNODE = "https://rpc-mainnet.matic.quiknode.pro/"
const val API_POLYGON_BWARELABS = "https://matic-mainnet-full-rpc.bwarelabs.com/"
const val API_POLYGON_TESTNET = "https://rpc-mumbai.maticvigil.com/"
//endregion

//region AVALANCHE API
const val API_AVALANCHE = "https://api.avax.network/ext/bc/C/rpc/"
const val API_AVALANCHE_NOWNODES = "https://avax.nownodes.io/ext/bc/C/rpc/"
const val API_AVALANCHE_GETBLOCK = "https://avax.getblock.io/mainnet/ext/bc/C/rpc/"
const val API_AVALANCHE_TESTNET = "https://api.avax-test.network/ext/bc/C/rpc/"
//endregion

//region FANTOM API
const val API_FANTOM_TOOLS = "https://rpc.ftm.tools/"
const val API_FANTOM_NETWORK = "https://rpcapi.fantom.network/"
const val API_FANTOM_ANKR_TOOLS = "https://rpc.ankr.com/fantom/"
const val API_FANTOM_ULTIMATENODES = "https://ftmrpc.ultimatenodes.io/"
const val API_FANTOM_NOWNODES = "https://ftm.nownodes.io/"
const val API_FANTOM_GETBLOCK = "https://ftm.getblock.io/mainnet/"
const val API_FANTOM_TESTNET = "https://rpc.testnet.fantom.network/"
//endregion

//region ARBITRUM API
const val API_ARBITRUM = "https://arb1.arbitrum.io/rpc/"
const val API_ARBITRUM_INFURA = "https://arbitrum-mainnet.infura.io/v3/"
const val API_ARBITRUM_OFFCHAIN = "https://node.offchainlabs.com:8547/"
const val API_ARBITRUM_TESTNET = "https://goerli-rollup.arbitrum.io/rpc/"
//endregion

//region GNOSIS API
const val API_GNOSIS_CHAIN = "https://rpc.gnosischain.com/"
const val API_GNOSIS_POKT = "https://gnosischain-rpc.gateway.pokt.network/"
const val API_GNOSIS_ANKR = "https://rpc.ankr.com/gnosis/"
const val API_GNOSIS_BLAST = "https://gnosis-mainnet.public.blastapi.io/"
const val API_GNOSIS_GETBLOCK = "https://gno.getblock.io/mainnet/"
const val API_XDAI_POKT = "https://xdai-rpc.gateway.pokt.network/"
const val API_XDAI_BLOCKSCOUT = "https://xdai-archive.blockscout.com/"
//endregion

//region ETHEREUM FAIR API
const val API_ETH_FAIR_RPC = "https://rpc.etherfair.org/"
//endregion

//region ETHEREUM POW API
const val API_ETH_POW = "https://mainnet.ethereumpow.org/"
const val API_ETH_POW_NOWNODES = "https://ethw.nownodes.io/"
const val API_ETH_POW_TESTNET = "https://iceberg.ethereumpow.org/"
//endregion

//region OPTIMISM API
const val API_OPTIMISM = "https://mainnet.optimism.io/"
const val API_OPTIMISM_BLAST = "https://optimism-mainnet.public.blastapi.io/"
const val API_OPTIMISM_ANKR = "https://rpc.ankr.com/optimism/"
const val API_OPTIMISM_NOWNODES = "https://optimism.nownodes.io/"
const val API_OPTIMISM_GETBLOCK = "https://op.getblock.io/mainnet/"
const val API_OPTIMISM_TESTNET = "https://goerli.optimism.io/"
//endregion

//region SALTPAY API
const val API_RPC_BICOCCACHAIN = "https://rpc.bicoccachain.net/"
const val API_BLOCKSCOUT_BICOCCACHAIN = "https://blockscout.bicoccachain.net/"
//endregion
