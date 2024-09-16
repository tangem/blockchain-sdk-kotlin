package com.tangem.blockchain.blockchains.icp.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

interface ICPNetworkProvider : NetworkProvider {
    suspend fun getBalance(address: String): Result<BigDecimal>

    suspend fun signAndSendTransaction(transferWithSigner: ICPTransferWithSigner): Result<Long?>
}

data class ICPTransferWithSigner(
    val transfer: ICPTransferRequest,
    val signer: TransactionSigner,
)