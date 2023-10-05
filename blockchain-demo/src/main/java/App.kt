package com.tangem.blockchain_demo

import android.app.Application
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        System.loadLibrary("TrustWalletCore")
        BlockchainSdkRetrofitBuilder.interceptors = listOf(ChuckerInterceptor(this))
    }
}