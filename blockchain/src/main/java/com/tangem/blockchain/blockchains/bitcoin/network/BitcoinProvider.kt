package com.tangem.blockchain.blockchains.bitcoin.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

interface BitcoinProvider {
    suspend fun getInfo(address: String): Result<BitcoinAddressInfo>
    suspend fun getFee(): Result<BitcoinFee>
    suspend fun sendTransaction(transaction: String): SimpleResult
    suspend fun getSignatureCount(address: String): Result<Int>
}