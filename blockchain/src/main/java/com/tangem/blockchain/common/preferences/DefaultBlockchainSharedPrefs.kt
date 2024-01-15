package com.tangem.blockchain.common.preferences

import android.content.Context

class DefaultBlockchainSharedPrefs(context: Context) : BlockchainSharedPrefs {
    private val sharedPrefs = context.getSharedPreferences(DEFAULT_BLOCKCHAIN_PREFS, Context.MODE_PRIVATE)

    override fun putString(key: String, value: String) {
        sharedPrefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String): String? {
        return sharedPrefs.getString(key, defaultValue)
    }

    companion object {
        private const val DEFAULT_BLOCKCHAIN_PREFS = "blockchain_prefs"
    }
}