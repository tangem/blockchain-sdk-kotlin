package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.common.extensions.toHexString
import org.kethereum.model.Transaction

data class ContractCallData(
    val to: String,
    val data: String,
) {
    companion object {
        private const val HEX_PREFIX = "0x"

        fun from(transaction: Transaction): ContractCallData {
            return ContractCallData(
                to = transaction.to!!.hex,
                data = HEX_PREFIX + transaction.input.toHexString(),
            )
        }
    }
}
