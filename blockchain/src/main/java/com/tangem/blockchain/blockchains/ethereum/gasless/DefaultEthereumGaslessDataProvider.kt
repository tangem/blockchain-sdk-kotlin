package com.tangem.blockchain.blockchains.ethereum.gasless

import com.tangem.blockchain.blockchains.ethereum.eip7702.EthEip7702Util
import com.tangem.blockchain.blockchains.ethereum.models.EIP7702AuthorizationData
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import java.math.BigInteger

internal class DefaultEthereumGaslessDataProvider(
    private val wallet: Wallet,
    private val networkProvider: EthereumNetworkProvider,
    private val gaslessContractAddressFactory: GaslessContractAddressFactory,
) : EthereumGaslessDataProvider {

    override suspend fun getGaslessContractNonce(userAddress: String): Result<BigInteger> {
        return networkProvider.getContractNonce(userAddress)
    }

    override suspend fun prepareEIP7702AuthorizationData(): Result<EIP7702AuthorizationData> {
        return try {
            val nonce = when (val nonceResult = networkProvider.getTxCount(wallet.address)) {
                is Result.Failure -> return Result.Failure(nonceResult.error)
                is Result.Success -> nonceResult.data
            }

            val chainId = wallet.blockchain.getChainId() ?: return Result.Failure(
                error = BlockchainSdkError.NPError("Chain id is null for ${wallet.blockchain}"),
            )
            // Encode and hash the EIP-7702 authorization data
            val data = EthEip7702Util.encodeAuthorizationForSigning(
                chainId = chainId,
                contractAddress = gaslessContractAddressFactory.getGaslessExecutorContractAddress(),
                nonce = nonce,
            )

            Result.Success(
                data = EIP7702AuthorizationData(
                    chainId = chainId,
                    nonce = nonce,
                    data = data,
                ),
            )
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override fun getExecutorContractAddress(): String {
        return gaslessContractAddressFactory.getGaslessExecutorContractAddress()
    }
}