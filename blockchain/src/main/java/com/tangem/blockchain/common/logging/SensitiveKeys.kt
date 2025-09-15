package com.tangem.blockchain.common.logging

import com.squareup.moshi.adapter
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.network.moshi
import org.json.JSONArray
import org.json.JSONObject

internal object SensitiveKeys {

    val data: List<String> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        initSensitiveKeys()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun initSensitiveKeys(): List<String> {
        val json = moshi.adapter<BlockchainSdkConfig>().toJson(DepsContainer.blockchainSdkConfig)
        return JSONObject(json).values()
    }

    /** Recursive function of searching all [String] values in JSON */
    private fun JSONObject.values(): List<String> {
        return keys().asSequence()
            .flatMap { key ->
                when (val value = opt(key)) {
                    is String -> listOf(value)
                    is JSONObject -> value.values()
                    is JSONArray -> (0..value.length()).mapNotNull { value.opt(it) as? String }
                    else -> emptyList()
                }
            }
            .filterNot(String::isBlank)
            .toList()
    }
}