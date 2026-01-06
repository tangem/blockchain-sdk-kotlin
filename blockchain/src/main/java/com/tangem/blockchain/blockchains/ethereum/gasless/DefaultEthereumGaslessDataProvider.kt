package com.tangem.blockchain.blockchains.ethereum.gasless

import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.extensions.Result
import java.math.BigInteger

class DefaultEthereumGaslessDataProvider(
    private val networkProvider: EthereumNetworkProvider,
) : EthereumGaslessDataProvider {

    override suspend fun getGaslessContractNonce(userAddress: String): Result<BigInteger> {
        return networkProvider.getContractNonce(userAddress)
    }
}