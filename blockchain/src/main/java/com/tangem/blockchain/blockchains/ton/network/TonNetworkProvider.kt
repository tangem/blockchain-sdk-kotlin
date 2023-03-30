package com.tangem.blockchain.blockchains.ton.network

import com.tangem.blockchain.extensions.Result

interface TonNetworkProvider {
    val host: String

    suspend fun getWalletInformation(address: String): Result<TonGetWalletInfoResponse>

    suspend fun getFee(address: String, message: String): Result<TonGetFeeResponse>

    suspend fun send(message: String): Result<TonSendBocResponse>
}