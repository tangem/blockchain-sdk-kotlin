package com.tangem.blockchain.blockchains.ton.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result

interface TonNetworkProvider: NetworkProvider {

    suspend fun getWalletInformation(address: String): Result<TonGetWalletInfoResponse>

    suspend fun getFee(address: String, message: String): Result<TonGetFeeResponse>

    suspend fun send(message: String): Result<TonSendBocResponse>
}
