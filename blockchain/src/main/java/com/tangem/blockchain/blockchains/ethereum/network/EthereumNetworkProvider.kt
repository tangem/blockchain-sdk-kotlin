package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.blockchair.BlockchairToken
import java.math.BigDecimal
import java.math.BigInteger

interface EthereumNetworkProvider : NetworkProvider {
    suspend fun getInfo(address: String, tokens: Set<Token>): Result<EthereumInfoResponse>
    suspend fun getPendingTxCount(address: String): Result<Long>
    suspend fun getTxCount(address: String): Result<BigInteger>
    suspend fun getAllowance(ownerAddress: String, token: Token, spenderAddress: String): kotlin.Result<BigDecimal>
    suspend fun sendTransaction(transaction: String): Result<String>
    suspend fun getSignatureCount(address: String): Result<Int>
    suspend fun findErc20Tokens(address: String): Result<List<BlockchairToken>>
    suspend fun getGasPrice(): Result<BigInteger>
    suspend fun getGasLimit(to: String, from: String, value: String?, data: String?): Result<BigInteger>

    /**
     * Estimate gas for an ERC20 `transferFrom` call where the spender currently has no
     * allowance, by overriding the token's `_allowances[owner][spender]` storage slot
     * to `uint256.max` in the simulation (JSON-RPC `stateDiff`).
     *
     * Default implementation returns a feature-disabled failure so existing concrete
     * providers and tests are not forced to implement state-override logic. The EVM
     * shared service overrides this method.
     */
    suspend fun getGasLimitWithAllowanceOverride(
        token: Token,
        ownerAddress: String,
        destinationAddress: String,
        spenderAddress: String,
        callData: SmartContractCallData,
    ): Result<BigInteger> = Result.Failure(
        BlockchainSdkError.CustomError(
            "State-override gas estimation is not supported by this network provider",
        ),
    )

    suspend fun getFeeHistory(): Result<EthereumFeeHistory>
    suspend fun getTokensBalance(address: String, tokens: Set<Token>): Result<List<Amount>>

    /**
     * Coin balance and the balances of [tokens] on an [address] that the caller's wallet does not own.
     *
     * Separate from [getInfo] and [getTokensBalance] on purpose. Both of those resolve token balances through the
     * yield-supply provider, which is built around the caller's own wallet, so on a foreign address they can answer
     * with the wallet's yield balance instead of that address's token balance. This one reads plain balances only,
     * and skips the transaction counts and the history that [getInfo] also fetches.
     *
     * Default implementation returns a feature-disabled failure so existing concrete providers and tests are not
     * forced to implement it. The EVM shared service overrides this method.
     */
    suspend fun getExternalAddressBalances(address: String, tokens: Set<Token>): Result<ExternalAddressBalances> =
        Result.Failure(
            BlockchainSdkError.CustomError(
                "Balances of an external address are not supported by this network provider",
            ),
        )

    suspend fun callContractForFee(data: ContractCallData): Result<BigInteger>
    suspend fun resolveName(namehash: ByteArray, encodedName: ByteArray): ResolveAddressResult
    suspend fun resolveAddress(address: String): ReverseResolveAddressResult

    /**
     * Get nonce from a smart contract for a specific user.
     * This is typically used for EIP-712 and EIP-7702 operations.
     *
     * @param address The address of the user.
     * @return Result containing the nonce or an error.
     */
    suspend fun getContractNonce(address: String): Result<BigInteger>
}

/**
 * Balances of an address, and nothing else — see [EthereumNetworkProvider.getExternalAddressBalances].
 *
 * @property coinBalance   the address's coin balance
 * @property tokenBalances the balances of the requested tokens, in the order the tokens were passed
 */
class ExternalAddressBalances(
    val coinBalance: BigDecimal,
    val tokenBalances: List<Amount>,
)

class EthereumInfoResponse(
    val coinBalance: BigDecimal,
    val tokenBalances: List<Amount>,
    val txCount: Long,
    val pendingTxCount: Long,
    val recentTransactions: List<TransactionData.Uncompiled>?,
)

sealed interface EthereumFeeHistory {

    val baseFee: BigDecimal

    data class Common(
        override val baseFee: BigDecimal,
        val lowPriorityFee: BigDecimal,
        val marketPriorityFee: BigDecimal,
        val fastPriorityFee: BigDecimal,
    ) : EthereumFeeHistory {

        fun toTriple() = Triple(lowPriorityFee, marketPriorityFee, fastPriorityFee)
    }

    data class Fallback(val gasPrice: BigInteger) : EthereumFeeHistory {
        override val baseFee: BigDecimal = BigDecimal.ZERO
    }
}