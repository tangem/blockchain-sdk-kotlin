package com.tangem.blockchain.blockchains.filecoin.network.converters

import com.tangem.blockchain.blockchains.filecoin.models.FilecoinTxInfo
import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinTransactionBody

/**
 * Converter from [FilecoinTxInfo] to [FilecoinTransactionBody]
 *
[REDACTED_AUTHOR]
 */
internal object FilecoinTransactionBodyConverter {

    fun convert(from: FilecoinTxInfo): FilecoinTransactionBody {
        return FilecoinTransactionBody(
            sourceAddress = from.sourceAddress,
            destinationAddress = from.destinationAddress,
            amount = from.amount.toString(),
            nonce = from.nonce,
            gasUnitPrice = null,
            gasLimit = null,
            gasPremium = null,
        )
    }
}