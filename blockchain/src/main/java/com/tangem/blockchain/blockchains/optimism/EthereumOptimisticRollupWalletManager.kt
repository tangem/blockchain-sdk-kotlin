package com.tangem.blockchain.blockchains.optimism

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.eip1559.isSupportEIP1559
import com.tangem.blockchain.blockchains.ethereum.network.ContractCallData
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumCompiledTxInfo
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.nft.DefaultNFTProvider
import com.tangem.blockchain.nft.NFTProvider
import com.tangem.blockchain.pendingtransactions.DefaultPendingTransactionsProvider
import com.tangem.blockchain.pendingtransactions.PendingTransactionsProvider
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.yieldsupply.DefaultYieldSupplyProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider
import org.kethereum.DEFAULT_GAS_LIMIT
import org.kethereum.model.Address
import java.math.BigDecimal
import java.math.BigInteger

data class L1GasOracleConfig(
    val address: String,
    val feeMultiplier: Double,
)

@Suppress("LongParameterList")
class EthereumOptimisticRollupWalletManager(
    wallet: Wallet,
    transactionBuilder: EthereumTransactionBuilder,
    networkProvider: EthereumNetworkProvider,
    private val l1GasOracleConfig: L1GasOracleConfig,
    transactionHistoryProvider: TransactionHistoryProvider = DefaultTransactionHistoryProvider,
    nftProvider: NFTProvider = DefaultNFTProvider,
    yieldSupplyProvider: YieldSupplyProvider = DefaultYieldSupplyProvider,
    pendingTransactionsProvider: PendingTransactionsProvider = DefaultPendingTransactionsProvider,
) : EthereumWalletManager(
    wallet = wallet,
    transactionBuilder = transactionBuilder,
    networkProvider = networkProvider,
    nftProvider = nftProvider,
    supportsENS = false,
    transactionHistoryProvider = transactionHistoryProvider,
    yieldSupplyProvider = yieldSupplyProvider,
    pendingTransactionsProvider = pendingTransactionsProvider,
) {

    private var lastLayer1FeeAmount: Amount? = null

    override suspend fun getFee(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData?,
    ): Result<TransactionFee.Choosable> {
        lastLayer1FeeAmount = null

        val layer2fee = super.getFee(amount, destination, callData).successOr {
            return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        } as? TransactionFee.Choosable ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)

        return if (wallet.blockchain.isSupportEIP1559) {
            getEIP1559Fee(
                amount = amount,
                destination = destination,
                data = callData?.dataHex,
                layer2fee = layer2fee,
            )
        } else {
            getLegacyFee(
                amount = amount,
                destination = destination,
                data = callData?.dataHex,
                layer2fee = layer2fee,
            )
        }
    }

    override suspend fun getFee(transactionData: TransactionData): Result<TransactionFee> {
        val uncompiledTransaction = transactionData.requireUncompiled()

        lastLayer1FeeAmount = null

        val extra = uncompiledTransaction.extras as? EthereumTransactionExtras

        val layer2fee = super.getFee(
            amount = uncompiledTransaction.amount,
            destination = uncompiledTransaction.destinationAddress,
            callData = extra?.callData,
        ).successOr {
            return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        } as? TransactionFee.Choosable ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)

        return if (wallet.blockchain.isSupportEIP1559) {
            getEIP1559Fee(
                amount = uncompiledTransaction.amount,
                destination = uncompiledTransaction.destinationAddress,
                data = extra?.callData?.dataHex,
                layer2fee = layer2fee,
            )
        } else {
            getLegacyFee(
                amount = uncompiledTransaction.amount,
                destination = uncompiledTransaction.destinationAddress,
                data = extra?.callData?.dataHex,
                layer2fee = layer2fee,
            )
        }
    }

    private suspend fun getLegacyFee(
        amount: Amount,
        destination: String,
        data: String?,
        layer2fee: TransactionFee.Choosable,
    ): Result<TransactionFee.Choosable> {
        val minimumFee = layer2fee.minimum as Fee.Ethereum.Legacy
        val normalFee = layer2fee.normal as Fee.Ethereum.Legacy
        val priorityFee = layer2fee.priority as Fee.Ethereum.Legacy

        val serializedTransaction = transactionBuilder.buildDummyTransactionForL1(
            amount = amount,
            destination = destination,
            data = data,
            fee = normalFee,
        )

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

    private suspend fun getEIP1559Fee(
        amount: Amount,
        destination: String,
        data: String?,
        layer2fee: TransactionFee.Choosable,
    ): Result<TransactionFee.Choosable> {
        val minimumFee = layer2fee.minimum as Fee.Ethereum.EIP1559
        val normalFee = layer2fee.normal as Fee.Ethereum.EIP1559
        val priorityFee = layer2fee.priority as Fee.Ethereum.EIP1559

        val serializedTransaction = transactionBuilder.buildDummyTransactionForL1(
            amount = amount,
            destination = destination,
            data = data,
            fee = normalFee,
        )

        val lastLayer1Fee = getLayer1Fee(serializedTransaction).successOr {
            return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        }
        lastLayer1FeeAmount = lastLayer1Fee

        // https://community.optimism.io/docs/developers/build/transaction-fees/#displaying-fees-to-users

        val lastLayer1FeeValue = requireNotNull(lastLayer1Fee.value) { "Fee must not bee null" }

        val updatedFees = layer2fee.copy(
            minimum = Fee.Ethereum.EIP1559(
                amount = minimumFee.amount + lastLayer1FeeValue,
                gasLimit = minimumFee.gasLimit,
                maxFeePerGas = minimumFee.maxFeePerGas,
                priorityFee = minimumFee.priorityFee,
            ),
            normal = Fee.Ethereum.EIP1559(
                amount = normalFee.amount + lastLayer1FeeValue,
                gasLimit = normalFee.gasLimit,
                maxFeePerGas = normalFee.maxFeePerGas,
                priorityFee = normalFee.priorityFee,
            ),
            priority = Fee.Ethereum.EIP1559(
                amount = priorityFee.amount + lastLayer1FeeValue,
                gasLimit = priorityFee.gasLimit,
                maxFeePerGas = priorityFee.maxFeePerGas,
                priorityFee = priorityFee.priorityFee,
            ),
        )

        return Result.Success(updatedFees)
    }

    override suspend fun sign(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<Pair<ByteArray, EthereumCompiledTxInfo>> {
        val uncompiledTransaction = transactionData.requireUncompiled()

        // For EIP-1559, the amount field is not used for fee calculation
        // (fee = gasLimit * maxFeePerGas), so we don't subtract L1 fee from amount.
        // For Legacy transactions, we subtract L1 fee from amount as it's deducted automatically.
        // https://help.optimism.io/hc/en-us/articles/4411895794715
        val calculatedTransactionFee = if (uncompiledTransaction.fee is Fee.Ethereum.EIP1559) {
            // EIP-1559: Don't subtract L1 fee from amount (not used anyway)
            uncompiledTransaction.fee.amount.value ?: BigDecimal.ZERO
        } else {
            // Legacy: Subtract L1 fee from amount
            (uncompiledTransaction.fee?.amount?.value ?: BigDecimal.ZERO) -
                (lastLayer1FeeAmount?.value ?: BigDecimal.ZERO)
        }

        val gasLimit = (uncompiledTransaction.extras as? EthereumTransactionExtras)?.gasLimit
            ?: (uncompiledTransaction.fee as? Fee.Ethereum)?.gasLimit
            ?: DEFAULT_GAS_LIMIT

        val updatedFee = when (val originalFee = uncompiledTransaction.fee) {
            is Fee.Ethereum.Legacy -> Fee.Ethereum.Legacy(
                amount = Amount(value = calculatedTransactionFee, blockchain = wallet.blockchain),
                gasLimit = gasLimit,
                gasPrice = originalFee.gasPrice,
            )
            is Fee.Ethereum.EIP1559 -> Fee.Ethereum.EIP1559(
                amount = Amount(value = calculatedTransactionFee, blockchain = wallet.blockchain),
                gasLimit = gasLimit,
                maxFeePerGas = originalFee.maxFeePerGas,
                priorityFee = originalFee.priorityFee,
            )
            else -> Fee.Ethereum.Legacy(
                amount = Amount(value = calculatedTransactionFee, blockchain = wallet.blockchain),
                gasLimit = gasLimit,
                gasPrice = BigInteger.ONE,
            )
        }

        val updatedTransactionData = uncompiledTransaction.copy(fee = updatedFee)
        return super.sign(updatedTransactionData, signer)
    }

    private suspend fun getLayer1Fee(serializedTransaction: ByteArray): Result<Amount> {
        return try {
            val transaction = OptimismGasL1TransactionGenerator(Address(l1GasOracleConfig.address))
                .getL1Fee(serializedTransaction)

            val contractCallData = ContractCallData.from(transaction)
            val fee: BigInteger = networkProvider.callContractForFee(contractCallData).successOr {
                return Result.Failure(BlockchainSdkError.FailedToLoadFee)
            }
            val feeIndexed = fee.toBigDecimal().movePointLeft(wallet.blockchain.decimals()) *
                BigDecimal.valueOf(l1GasOracleConfig.feeMultiplier)
            val amount = Amount(
                blockchain = wallet.blockchain,
                value = feeIndexed,
            )
            Result.Success(amount)
        } catch (error: Throwable) {
            Result.Failure(BlockchainSdkError.WrappedThrowable(error))
        }
    }
}