package com.tangem.blockchain.blockchains.near.network

import com.tangem.blockchain.blockchains.near.NearGasPrice
import com.tangem.blockchain.blockchains.near.NearWalletInfo
import com.tangem.blockchain.blockchains.near.SendTransactionResult
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class NearNetworkService(
    private val multiJsonRpcProvider: MultiNetworkProvider<NearJsonRpcNetworkProvider>,
    private val blockchain: Blockchain,
) {

    val host: String get() = multiJsonRpcProvider.currentProvider.host

    suspend fun getAccount(address: String): Result<NearWalletInfo> {
        val accountResult = multiJsonRpcProvider.performRequest(NearNetworkProvider::getAccount, address)
            .successOr { return it }

        val nearWalletInfo = NearWalletInfo(
            amount = BigDecimal(accountResult.amount).movePointLeft(blockchain.decimals()),
            blockHash = accountResult.blockHash,
            blockHeight = accountResult.blockHeight,
        )

        return Result.Success(nearWalletInfo)
    }

    suspend fun getGas(blockHeight: Long): Result<NearGasPrice> {
        val gasPriceResult = multiJsonRpcProvider.performRequest(NearNetworkProvider::getGas, blockHeight)
            .successOr { return it }

        val nearWalletInfo = NearGasPrice(
            gasPrice = BigDecimal(gasPriceResult.gasPrice).movePointLeft(blockchain.decimals()),
            blockHeight = blockHeight,
        )

        return Result.Success(nearWalletInfo)
    }

    suspend fun sendTransaction(signedTxBase64: String): Result<SendTransactionResult> {
        val sendTxResult = multiJsonRpcProvider.performRequest(NearNetworkProvider::sendTransaction, signedTxBase64)
            .successOr { return it }

        val nearWalletInfo = SendTransactionResult(
            hash = sendTxResult
        )

        return Result.Success(nearWalletInfo)
    }
}