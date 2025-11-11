package com.tangem.blockchain.blockchains.ton.network

import com.tangem.blockchain.blockchains.ton.models.GetJettonWalletAddressInput
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import java.math.BigInteger

interface TonNetworkProvider : NetworkProvider {

    suspend fun getWalletInformation(address: String): Result<TonGetWalletInfoResponse>

    suspend fun getFee(address: String, message: String): Result<TonGetFeeResponse>

    suspend fun send(message: String): Result<TonSendBocResponse>

    suspend fun getJettonWalletAddress(input: GetJettonWalletAddressInput): Result<String>

    suspend fun getJettonBalance(jettonWalletAddress: String): Result<BigInteger>

    suspend fun isJettonWalletActive(jettonWalletAddress: String): Result<Boolean>
}