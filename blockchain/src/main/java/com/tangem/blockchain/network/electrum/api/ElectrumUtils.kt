package com.tangem.blockchain.network.electrum.api

import android.os.SystemClock
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map

internal suspend fun ElectrumApiService.getLatencyMillis(): Result<Long> {
    val now = SystemClock.elapsedRealtime()
    val res = getBlockTip()

    return res.map {
        SystemClock.elapsedRealtime() - now
    }
}