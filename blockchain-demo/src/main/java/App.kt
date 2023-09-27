package com.tangem.blockchain_demo

import android.app.Application

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        System.loadLibrary("TrustWalletCore")
    }
}