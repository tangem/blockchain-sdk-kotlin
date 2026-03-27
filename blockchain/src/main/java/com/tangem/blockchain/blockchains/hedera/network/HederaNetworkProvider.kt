package com.tangem.blockchain.blockchains.hedera.network

import com.tangem.blockchain.blockchains.hedera.models.HederaTokenType
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

internal interface HederaNetworkProvider : NetworkProvider {

    suspend fun getAccountId(publicKey: ByteArray): Result<String>

    suspend fun getUsdExchangeRate(): Result<BigDecimal>

    suspend fun getBalances(accountId: String): Result<HederaBalancesResponse>

    suspend fun getTransactionInfo(transactionId: String): Result<HederaTransactionResponse>

    suspend fun getTokenDetails(tokenId: String): Result<HederaTokenDetailsResponse>

    suspend fun getTokenType(tokenId: String): Result<HederaTokenType>

    suspend fun getContractInfo(contractIdOrAddress: String): Result<HederaContractResponse>

    suspend fun invokeSmartContract(request: HederaContractCallRequest): Result<HederaContractCallResponse>

    suspend fun getNetworkFees(): Result<HederaNetworkFeesResponse>

    suspend fun getAccountDetail(idOrAddress: String): Result<HederaAccountDetailResponse>
}