package com.tangem.blockchain.blockchains.optimism

import com.tangem.blockchain.blockchains.ethereum.CompiledEthereumTransaction
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.ContractCallData
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.extensions.hexToBytes
import org.kethereum.DEFAULT_GAS_LIMIT
import org.kethereum.extensions.transactions.encode
import org.kethereum.model.Address
import org.kethereum.model.createTransactionWithDefaults
import java.math.BigDecimal
import java.math.BigInteger

class EthereumOptimisticRollupWalletManager(
    wallet: Wallet,
    transactionBuilder: EthereumTransactionBuilder,
    networkProvider: EthereumNetworkProvider,
) : EthereumWalletManager(wallet, transactionBuilder, networkProvider) {

    private var lastLayer1FeeAmount: Amount? = null

    @Suppress("MagicNumber")
    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee.Choosable> {
        lastLayer1FeeAmount = null

        val layer2fee = super.getFee(amount, destination).successOr {
            return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        } as? TransactionFee.Choosable ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)

        val minimumFee = layer2fee.minimum as Fee.Ethereum.Legacy
        val normalFee = layer2fee.normal as Fee.Ethereum.Legacy
        val priorityFee = layer2fee.priority as Fee.Ethereum.Legacy

        val serializedTransaction = createTransactionWithDefaults(
            from = Address(wallet.address),
            to = Address(destination),
            value = BigInteger.valueOf(1), // value is not important for dummy transactions
            gasPrice = normalFee.gasPrice,
            gasLimit = normalFee.gasLimit,
            nonce = BigInteger.ONE,
        ).encode()

        val lastLayer1Fee = getLayer1Fee(serializedTransaction).successOr {
            return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        }
        lastLayer1FeeAmount = lastLayer1Fee

        // https://community.optimism.io/docs/developers/build/transaction-fees/#displaying-fees-to-users

        val lastLayer1FeeValue = requireNotNull(lastLayer1Fee.value) { "Fee must not bee null" }

        val updatedFees = layer2fee.copy(
            minimum = Fee.Ethereum.Legacy(
                amount = minimumFee.amount + lastLayer1FeeValue,
                gasLimit = minimumFee.gasLimit,
                gasPrice = minimumFee.gasPrice,
            ),
            normal = Fee.Ethereum.Legacy(
                amount = normalFee.amount + lastLayer1FeeValue,
                gasLimit = normalFee.gasLimit,
                gasPrice = normalFee.gasPrice,
            ),
            priority = Fee.Ethereum.Legacy(
                amount = priorityFee.amount + lastLayer1FeeValue,
                gasLimit = priorityFee.gasLimit,
                gasPrice = priorityFee.gasPrice,
            ),
        )

        return Result.Success(updatedFees)
    }

    @Suppress("MagicNumber")
    override suspend fun getFee(amount: Amount, destination: String, data: String): Result<TransactionFee.Choosable> {
        lastLayer1FeeAmount = null

        val blockchain = wallet.blockchain
        val layer2fee = super.getFee(amount, destination, data).successOr {
            return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        } as? TransactionFee.Choosable ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)

        val extras = layer2fee.minimum as Fee.Ethereum.Legacy

        val preparedAmount = Amount(
            value = BigDecimal.valueOf(0.1),
            blockchain = blockchain,
        )

        val gasPriceL2 = extras.gasPrice
        val gasLimitL2 = extras.gasLimit
        val value = preparedAmount.value?.movePointRight(preparedAmount.decimals)?.toBigInteger() ?: BigInteger.ZERO

        // creating sample transaction that hash should be send to optimism contract to determine layer1Fee
        val serializedTransaction = createTransactionWithDefaults(
            from = Address(wallet.address),
            to = Address(destination),
            value = value,
            gasPrice = gasPriceL2,
            gasLimit = gasLimitL2,
            nonce = BigInteger.ONE,
            input = data.removePrefix(HEX_PREFIX).hexToBytes(),
        ).encode()

        val lastLayer1Fee = getLayer1Fee(serializedTransaction).successOr {
            return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        }
        lastLayer1FeeAmount = lastLayer1Fee

        // https://community.optimism.io/docs/developers/build/transaction-fees/#displaying-fees-to-users
        val lastLayer1FeeValue = requireNotNull(lastLayer1Fee.value) { "Fee must not bee null" }
        val updatedFees = layer2fee.copy(
            minimum = Fee.Ethereum.Legacy(
                amount = layer2fee.minimum.amount + lastLayer1FeeValue,
                gasLimit = layer2fee.minimum.gasLimit,
                gasPrice = layer2fee.minimum.gasPrice,
            ),
            normal = Fee.Ethereum.Legacy(
                amount = (layer2fee.normal as Fee.Ethereum.Legacy).amount + lastLayer1FeeValue,
                gasLimit = layer2fee.normal.gasLimit,
                gasPrice = layer2fee.normal.gasPrice,
            ),
            priority = Fee.Ethereum.Legacy(
                amount = (layer2fee.priority as Fee.Ethereum.Legacy).amount + lastLayer1FeeValue,
                gasLimit = layer2fee.priority.gasLimit,
                gasPrice = layer2fee.priority.gasPrice,
            ),
        )

        return Result.Success(updatedFees)
    }

    override suspend fun sign(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<Pair<ByteArray, CompiledEthereumTransaction>> {
        transactionData.requireUncompiled()

        // We need to subtract layer 1 fee, because it is deducted automatically
        // and should not be included into transaction for signing
        // https://help.optimism.io/hc/en-us/articles/4411895794715
        val calculatedTransactionFee = (transactionData.fee?.amount?.value ?: BigDecimal.ZERO) -
            (lastLayer1FeeAmount?.value ?: BigDecimal.ZERO)

        val gasLimit = (transactionData.extras as? EthereumTransactionExtras)?.gasLimit
            ?: (transactionData.fee as? Fee.Ethereum.Legacy)?.gasLimit
            ?: DEFAULT_GAS_LIMIT

        val updatedTransactionData = transactionData.copy(
            fee = Fee.Ethereum.Legacy(
                amount = Amount(value = calculatedTransactionFee, blockchain = wallet.blockchain),
                gasLimit = gasLimit,
                gasPrice = BigInteger.ONE,
            ),
        )
        return super.sign(updatedTransactionData, signer)
    }

    private suspend fun getLayer1Fee(transactionHash: ByteArray): Result<Amount> {
        return try {
            val transaction = OptimismGasL1TransactionGenerator(Address(OPTIMISM_FEE_CONTRACT_ADDRESS))
                .getL1Fee(transactionHash)

            val contractCallData = ContractCallData.from(transaction)
            val fee: BigInteger = networkProvider.callContractForFee(contractCallData).successOr {
                return Result.Failure(BlockchainSdkError.FailedToLoadFee)
            }
            val feeIndexed = fee.toBigDecimal().movePointLeft(wallet.blockchain.decimals()) *
                BigDecimal.valueOf(OPTIMISM_FEE_MULTIPLIER)
            val amount = Amount(
                blockchain = wallet.blockchain,
                value = feeIndexed,
            )
            Result.Success(amount)
        } catch (error: Throwable) {
            Result.Failure(BlockchainSdkError.WrappedThrowable(error))
        }
    }

    companion object {
        private const val OPTIMISM_FEE_CONTRACT_ADDRESS = "0x420000000000000000000000000000000000000F"
        private const val OPTIMISM_FEE_MULTIPLIER = 1.1
        private const val HEX_PREFIX = "0x"
    }
}