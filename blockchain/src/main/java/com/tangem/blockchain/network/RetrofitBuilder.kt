package com.tangem.blockchain.network

import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.blockchain.blockchains.aptos.network.response.AptosResource
import com.tangem.blockchain.blockchains.aptos.network.response.AptosResourceBodyAdapter
import com.tangem.blockchain.blockchains.filecoin.network.response.FilecoinRpcResponse
import com.tangem.blockchain.blockchains.filecoin.network.response.FilecoinRpcResponseAdapter
import com.tangem.blockchain.common.EnumeratedEnum
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.PolygonScanResultAdapter
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

fun createRetrofitInstance(baseUrl: String, headerInterceptors: List<Interceptor> = emptyList()): Retrofit =
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
            call = Timeout(time = 10),
            connect = Timeout(time = 20),
            // increased timeouts to receive response for user with a lot inputs
            // part of task [REDACTED_JIRA]
            read = Timeout(time = 90),
            write = Timeout(time = 90),
        )
    }
}

data class Timeout(
    val time: Long,
    val unit: TimeUnit = TimeUnit.SECONDS,
)

internal val moshi: Moshi by lazy {
    Moshi.Builder()
        .add(BigDecimal::class.java, BigDecimalAdapter)
        .add(AptosResource::class.java, AptosResourceBodyAdapter)
        .add(FilecoinRpcResponse::class.java, FilecoinRpcResponseAdapter)
        .add(createEnumJsonAdapter<GetAddressResponse.Transaction.StatusType>())
        .add(PolygonScanResultAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()
}

inline fun <reified T> createEnumJsonAdapter(): JsonAdapter<T> where T : Enum<T>, T : EnumeratedEnum {
    return object : JsonAdapter<T>() {
        @FromJson
        override fun fromJson(reader: JsonReader): T? {
            return if (reader.peek() != JsonReader.Token.NULL) {
                val value = reader.nextInt()
                enumValues<T>().firstOrNull { it.value == value }
            } else {
                reader.nextNull()
            }
        }

        @ToJson
        override fun toJson(writer: JsonWriter, value: T?) {
            writer.value(value?.value)
        }
    }
}

const val API_TANGEM = "https://verify.tangem.com/"
const val API_COINMARKETCAP = "https://pro-api.coinmarketcap.com/"
const val API_SOCHAIN_V2 = "https://chain.so/"
const val API_UPDATE_VERSION = "https://raw.githubusercontent.com/"
const val API_BLOCKCYPHER = "https://api.blockcypher.com/"
const val API_BLOCKCKAIR_TANGEM = "https://api.tangem-tech.com/"
const val API_BINANCE = "https://dex.binance.org/"
const val API_BINANCE_TESTNET = "https://testnet-dex.binance.org/"
const val API_STELLAR_RESERVE = "https://horizon.sui.li/"
const val API_ADALITE = "https://explorer2.adalite.io/"
const val API_XRP_LEDGER_FOUNDATION = "https://xrplcluster.com/"
const val API_BLOCKCHAIR = "https://api.blockchair.com/"
const val API_TEZOS_BLOCKSCALE = "https://rpc.tzbeta.net/"
const val API_TEZOS_SMARTPY = "https://mainnet.smartpy.io/"
const val API_TEZOS_ECAD = "https://api.tez.ie/rpc/mainnet/"
const val API_TEZOS_MARIGOLD = "https://mainnet.tezos.marigold.dev"
const val API_DUCATUS = "https://ducapi.rocknblock.io/"
const val API_BITCOINFEES_EARN = "https://bitcoinfees.earn.com/"
const val API_KASPA = "https://api.kaspa.org/"
const val API_CHIA_FIREACADEMY = "https://kraken.fireacademy.io/leaflet/"
const val API_CHIA_FIREACADEMY_TESTNET = "https://kraken.fireacademy.io/leaflet-testnet10/"
const val API_CHIA_TANGEM = "https://chia.tangem.com/"
const val API_HEDERA_MIRROR_TESTNET = "https://testnet.mirrornode.hedera.com/api/v1/"
const val API_HEDERA_ARKHIA_MIRROR = "https://pool.arkhia.io/hedera/mainnet/api/v1/"
const val API_HEDERA_ARKHIA_MIRROR_TESTNET = "https://pool.arkhia.io/hedera/testnet/api/v1/"