package com.tangem.blockchain.blockchains.hedera

import com.tangem.blockchain.blockchains.hedera.network.HederaMirrorRestProvider
import com.tangem.blockchain.extensions.Result

class HederaContractIdResolver(baseUrl: String) {

    private val provider = HederaMirrorRestProvider(baseUrl = baseUrl)

    suspend fun resolve(evmAddress: String): String? {
        return when (val result = provider.getContractInfo(evmAddress)) {
            is Result.Success -> result.data.contractId
            is Result.Failure -> null
        }
    }
}