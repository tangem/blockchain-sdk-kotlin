package com.tangem.blockchain.blockchains.ethereum.gasless

import com.tangem.blockchain.blockchains.ethereum.models.EIP7702AuthorizationData
import com.tangem.blockchain.extensions.Result
import java.math.BigInteger

/**
 * Interface for working with Ethereum typed data (EIP-712) and authorization data (EIP-7702).
 * Provides methods to retrieve contract nonce and prepare typed data for signing.
 */
interface EthereumGaslessDataProvider {

    /**
     * Get the current nonce for a smart contract related to a user address.
     *
     * @param userAddress The address of the user.
     * @return Result containing the nonce value or an error.
     */
    suspend fun getGaslessContractNonce(userAddress: String): Result<BigInteger>

    /**
     * Prepare EIP-7702 authorization data for attaching a gasless contract to user's account.
     * This method retrieves the nonce and prepares the hash to be signed.
     *
     * @return Result [EIP7702AuthorizationData]
     */
    suspend fun prepareEIP7702AuthorizationData(): Result<EIP7702AuthorizationData>

    fun getExecutorContractAddress(): String
}