package com.tangem.blockchain.blockchains.xrp.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

interface XrpNetworkService {
    suspend fun getInfo(address: String): Result<XrpInfoResponse>
    suspend fun sendTransaction(transaction: String): SimpleResult
    suspend fun getFee(): Result<XrpFeeResponse>
    suspend fun checkIsAccountCreated(address: String): Boolean
}