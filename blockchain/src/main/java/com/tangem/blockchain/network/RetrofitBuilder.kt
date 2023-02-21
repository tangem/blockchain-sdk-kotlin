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