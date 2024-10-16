package com.tangem.blockchain.blockchains.filecoin.network.converters

import com.tangem.blockchain.blockchains.filecoin.models.FilecoinTxGasInfo
import com.tangem.blockchain.blockchains.filecoin.network.response.FilecoinRpcResponseResult

/**
 * Convert from [FilecoinRpcResponseResult.GetMessageGas] to [FilecoinTxGasInfo]
 *
 * @author Andrew Khokhlov on 02/08/2024
 */
internal object FilecoinTxGasInfoConverter {

    fun convert(from: FilecoinRpcResponseResult.GetMessageGas): FilecoinTxGasInfo {
        return with(from) {
            FilecoinTxGasInfo(
                gasUnitPrice = gasUnitPrice.toLong(),
                gasLimit = gasLimit,
                gasPremium = gasPremium.toLong(),
            )
        }
    }
}
