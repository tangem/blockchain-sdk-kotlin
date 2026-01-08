package com.tangem.blockchain.blockchains.ethereum.gasless

import com.tangem.blockchain.blockchains.ethereum.eip7702.EthEip7702Util
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import java.math.BigInteger

internal class DefaultEthereumGaslessDataProvider(
    private val networkProvider: EthereumNetworkProvider,
    private val gaslessContractAddressFactory: GaslessContractAddressFactory,
) : EthereumGaslessDataProvider {

    override suspend fun getGaslessContractNonce(userAddress: String): Result<BigInteger> {
        return networkProvider.getContractNonce(userAddress)
    }

    override suspend fun prepareEIP7702AuthorizationData(chainId: Int, nonce: BigInteger): Result<ByteArray> {
        return try {
            // Encode and hash the EIP-7702 authorization data
            val hash = EthEip7702Util.encodeAuthorizationForSigning(
                chainId = chainId,
                contractAddress = gaslessContractAddressFactory.getGaslessExecutorContractAddress(),
                nonce = nonce,
            )

            Result.Success(hash)
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override fun getExecutorContractAddress(): String {
        return gaslessContractAddressFactory.getGaslessExecutorContractAddress()
    }
}