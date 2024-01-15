package com.tangem.demo

import android.app.Application
import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        System.loadLibrary("TrustWalletCore")
        BlockchainSdkRetrofitBuilder.interceptors = listOf(ChuckerInterceptor(this))
        context = this
    }

    companion object {
        lateinit var context: Context
    }
}