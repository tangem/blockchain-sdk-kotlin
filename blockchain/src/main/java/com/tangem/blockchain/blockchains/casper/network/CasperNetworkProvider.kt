package com.tangem.blockchain.blockchains.casper.network

import com.tangem.blockchain.blockchains.casper.models.CasperBalance
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result

internal interface CasperNetworkProvider : NetworkProvider {
    suspend fun getBalance(address: String): Result<CasperBalance>
}