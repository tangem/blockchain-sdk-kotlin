package com.tangem.blockchain.blockchains.litecoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.extensions.Result

class LitecoinNetworkService(providers: List<BitcoinNetworkProvider>) : BitcoinNetworkService(providers) {

    override suspend fun getFee(): Result<BitcoinFee> {
        val result = super.getFee()
        return if (result is Result.Success) {
            Result.Success(
                result.data.copy(
                    minimalPerKb = BitcoinWalletManager.DEFAULT_MINIMAL_FEE_PER_KB.toBigDecimal(),
                ),
            )
        } else {
            result
        }
    }
}
