package com.tangem.blockchain.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.blockchain.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object BlockchainSdkRetrofitBuilder {

    var enableNetworkLogging: Boolean = false

    internal val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG || enableNetworkLogging) addInterceptor(createHttpLoggingInterceptor())
        }.build()
    }

    private fun createHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        return logging
    }


}


private val moshi: Moshi by lazy {
    Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
}

fun createRetrofitInstance(baseUrl: String): Retrofit =
        Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(BlockchainSdkRetrofitBuilder.okHttpClient)
                .build()

const val API_TANGEM = "https://verify.tangem.com/"
const val API_COINMARKETCAP = "https://pro-api.coinmarketcap.com/"
const val API_INFURA = "https://mainnet.infura.io/"
const val API_INFURA_TESTNET = "https://rinkeby.infura.io/"
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
const val API_ARBITRUM_TESTNET = "https://rinkeby.arbitrum.io/"
const val API_GNOSIS_CHAIN = "https://rpc.gnosischain.com/"
const val API_GNOSIS_POKT = "https://gnosischain-rpc.gateway.pokt.network/"
const val API_GNOSIS_ANKR = "https://rpc.ankr.com/gnosis/"
const val API_GNOSIS_BLAST = "https://gnosis-mainnet.public.blastapi.io/"
const val API_XDAI_POKT = "https://xdai-rpc.gateway.pokt.network/"
const val API_XDAI_BLOCKSCOUT = "https://xdai-archive.blockscout.com/"
const val API_ETH_FAIR_RPC = "https://rpc.etherfair.org"
const val API_ETH_POW_RPC = "https://mainnet.ethereumpow.org"
const val API_ETH_POW_TESTNET_RPC = "https://iceberg.ethereumpow.org"
