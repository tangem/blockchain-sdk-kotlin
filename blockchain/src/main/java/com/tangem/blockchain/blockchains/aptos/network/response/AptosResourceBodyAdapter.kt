package com.tangem.blockchain.blockchains.aptos.network.response

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.adapter
import com.tangem.blockchain.network.moshi
import java.math.BigDecimal
import kotlin.properties.Delegates

@OptIn(ExperimentalStdlibApi::class)
internal object AptosResourceBodyAdapter : JsonAdapter<AptosResource>() {

    private const val COIN_RESOURCE_TYPE_PREFIX = "0x1::coin::CoinStore"
    private const val ACCOUNT_RESOURCE_TYPE = "0x1::account::Account"
    private const val NATIVE_COIN_RESOURCE_TYPE = "$COIN_RESOURCE_TYPE_PREFIX<0x1::aptos_coin::AptosCoin>"

    private val options = JsonReader.Options.of("type", "data")

    private val stringAdapter by lazy { moshi.adapter<String>() }
    private val accountDataAdapter by lazy { moshi.adapter<AptosAccountDataBody>() }
    private val coinDataAdapter by lazy { moshi.adapter<AptosCoinDataBody>() }

    @Suppress("CyclomaticComplexMethod")
    override fun fromJson(reader: JsonReader): AptosResource {
        var type by Delegates.notNull<String>()
        var accountResource: AptosAccountDataBody? = null
        var coinData: AptosCoinDataBody? = null
        var tokenData: AptosCoinDataBody? = null

        reader.beginObject()

        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> {
                    type = stringAdapter.fromJson(reader)
                        ?: error("Failed to parse resource type")
                }
                1 -> {
                    when (type) {
                        ACCOUNT_RESOURCE_TYPE -> {
                            accountResource = accountDataAdapter.fromJson(reader)
                                ?: error("Failed to parse account data")
                        }
                        NATIVE_COIN_RESOURCE_TYPE -> {
                            coinData = coinDataAdapter.fromJson(reader)
                                ?: error("Failed to parse native coin data")
                        }
                        else -> {
                            if (type.startsWith(COIN_RESOURCE_TYPE_PREFIX)) {
                                tokenData = coinDataAdapter.fromJson(reader)
                                    ?: error("Failed to parse native coin data")
                            } else {
                                reader.skipValue()
                            }
                        }
                    }
                }
                -1 -> reader.skipNameAndValue()
            }
        }

        reader.endObject()

        return accountResource?.toAccountResource()
            ?: coinData?.toCoinResource()
            ?: tokenData?.toTokenResource(type)
            ?: AptosResource.Unknown(type)
    }

    override fun toJson(writer: JsonWriter, value: AptosResource?) {
        error("Not used")
    }

    private fun JsonReader.skipNameAndValue() {
        skipName()
        skipValue()
    }

    private fun AptosAccountDataBody.toAccountResource(): AptosResource.AccountResource {
        return AptosResource.AccountResource(sequenceNumber)
    }

    private fun AptosCoinDataBody.toCoinResource(): AptosResource.CoinResource {
        return AptosResource.CoinResource(balance = BigDecimal(coin.value))
    }

    private fun AptosCoinDataBody.toTokenResource(type: String): AptosResource.TokenResource {
        return AptosResource.TokenResource(
            contractAddress = type.parseContractAddress(),
            balance = BigDecimal(coin.value),
        )
    }

    private fun String.parseContractAddress(): String {
        return this.substringAfter(delimiter = "<").substringBefore(delimiter = ">")
    }
}
