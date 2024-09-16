package com.tangem.blockchain.blockchains.icp

import com.tangem.blockchain.blockchains.icp.network.ICPAmount
import com.tangem.blockchain.blockchains.icp.network.ICPTimestamp
import com.tangem.blockchain.blockchains.icp.network.ICPTransferRequest
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.extensions.hexToBytes

internal class ICPTransactionBuilder(blockchain: Blockchain) {
    val decimals = blockchain.decimals()
    fun buildForSign(transactionData: TransactionData, timestampNanos: Long): ICPTransferRequest {
        transactionData.requireUncompiled()

        return ICPTransferRequest(
            to = transactionData.destinationAddress.hexToBytes(),
            amount = ICPAmount(transactionData.amount.longValue!!),
            fee = ICPAmount(transactionData.fee?.amount?.longValue ?: 0),
            memo = (transactionData.extras as? ICPTransactionExtras)?.memo ?: 0,
            createdAtTime = ICPTimestamp(timestampNanos),
        )
    }
}