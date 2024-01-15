package com.tangem.blockchain.common.preferences

interface BlockchainSharedPrefs {
    fun putString(key: String, value: String)

    fun getString(key: String, defaultValue: String): String?
}