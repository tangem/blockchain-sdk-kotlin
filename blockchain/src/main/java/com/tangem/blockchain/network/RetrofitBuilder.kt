package com.tangem.blockchain.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.blockchain.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder().apply {
        if (BuildConfig.DEBUG) addInterceptor(createHttpLoggingInterceptor())
    }.build()
}

private fun createHttpLoggingInterceptor(): HttpLoggingInterceptor {
    val logging = HttpLoggingInterceptor()
    logging.level = HttpLoggingInterceptor.Level.BODY
    return logging
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
                .client(okHttpClient)
                .build()


const val API_TANGEM = "https://verify.tangem.com/"
const val API_COINMARKETCAP = "https://pro-api.coinmarketcap.com/"
const val API_INFURA = "https://mainnet.infura.io/"
const val API_INFURA_TESTNET = "https://rinkeby.infura.io/"
const val API_SOCHAIN_V2 = "https://chain.so/"
const val API_UPDATE_VERSION = "https://raw.githubusercontent.com/"
const val API_RSK = "https://public-node.rsk.co/"
const val API_BLOCKCYPHER = "https://api.blockcypher.com/"
const val API_BINANCE = "https://dex.binance.org/"
const val API_BINANCE_TESTNET = "https://testnet-dex.binance.org/"
const val API_MATIC_TESTNET = "https://testnet2.matic.network/"
const val API_STELLAR = "https://horizon.stellar.org/"
const val API_STELLAR_RESERVE = "https://horizon.sui.li/"
const val API_STELLAR_TESTNET = "https://horizon-testnet.stellar.org/"
const val API_BLOCKCHAIN_INFO = "https://blockchain.info/"
const val API_ADALITE = "https://explorer2.adalite.io/"
const val API_ROSETTA = "http://217.107.110.2:8081/"
const val API_RIPPLED = "https://s1.ripple.com:51234/"
const val API_RIPPLED_RESERVE = "https://s2.ripple.com:51234/"
const val API_BLOCKCHAIR = "https://api.blockchair.com/"
const val API_TEZOS_LETZBAKE = "https://teznode.letzbake.com/"
const val API_TEZOS_BLOCKSCALE = "https://rpc.tzbeta.net/"
const val API_TEZOS_SMARTPY = "https://mainnet.smartpy.io/"
const val API_TEZOS_ECAD = "https://api.tez.ie/rpc/mainnet/"
const val API_DUCATUS = "https://ducapi.rocknblock.io/"
const val API_BITCOINFEES_EARN = "https://bitcoinfees.earn.com/"
const val API_TANGEM_ETHEREUM = "https://eth.tangem.com/"
const val API_TANGEM_ROSETTA = "https://ada.tangem.com/"