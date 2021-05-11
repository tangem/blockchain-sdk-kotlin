package com.tangem.blockchain.blockchains.litecoin

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.extensions.Result

class LitecoinNetworkService(
        providers: List<BitcoinNetworkProvider>
) : BitcoinNetworkService(providers) {

    override suspend fun getFee(): Result<BitcoinFee> {
        val result = currentProvider.getFee()
        return when {
            result.needsRetry() -> getFee()
            result is Result.Success -> {
                Result.Success(result.data.copy(minimalPerKb = 0.00001024.toBigDecimal()))
            }
            else -> result
        }
    }
}