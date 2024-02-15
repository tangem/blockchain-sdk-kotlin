package com.tangem.blockchain.blockchains.hedera.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

interface HederaNetworkProvider : NetworkProvider {

    suspend fun getAccountId(publicKey: ByteArray): Result<String>

    suspend fun getUsdExchangeRate(): Result<BigDecimal>
}