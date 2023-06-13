package com.tangem.blockchain.blockchains.optimism

import com.tangem.blockchain.blockchains.ethereum.CompiledEthereumTransaction
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.ContractCallData
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.extensions.hexToBytes
import org.kethereum.extensions.transactions.encode
import org.kethereum.model.Address
import org.kethereum.model.SignatureData
import org.kethereum.model.createTransactionWithDefaults
import java.math.BigDecimal
import java.math.BigInteger

class OptimismWalletManager(
    wallet: Wallet,
    transactionBuilder: EthereumTransactionBuilder,
    networkProvider: EthereumNetworkProvider,
    presetTokens: MutableSet<Token>,
) : EthereumWalletManager(wallet, transactionBuilder, networkProvider, presetTokens) {

    private var lastLayer1FeeAmount: Amount? = null

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee.Choosable> {
        lastLayer1FeeAmount = null

        val blockchain = wallet.blockchain
        val layer2fee = super.getFee(amount, destination).successOr {
            return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        }
        val preparedAmount = Amount(
            value = BigDecimal.valueOf(0.1),
            type = amount.type,
            blockchain = blockchain
        )

        val transactionData = TransactionData(
            amount = preparedAmount,
            fee = Fee.CommonFee(preparedAmount), // value is not important for dummy transactions
            sourceAddress = wallet.address,
            destinationAddress = destination,
        )

        val transaction = transactionBuilder.buildToSign(
            transactionData = transactionData,
            nonce = BigInteger.ONE
        )?.hash ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)

        val lastLayer1Fee = getLayer1Fee(transaction).successOr {
            return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        }
        lastLayer1FeeAmount = lastLayer1Fee

        //https://community.optimism.io/docs/developers/build/transaction-fees/#displaying-fees-to-users

        val updatedFees = layer2fee.copy(
            minimum = Fee.EthereumFee(
                amount = (layer2fee.minimum as Fee.EthereumFee).amount + requireNotNull(lastLayer1Fee.value),
                gasLimit = layer2fee.minimum.gasLimit,
                gasPrice = layer2fee.minimum.gasPrice
            ),
            normal = Fee.EthereumFee(
                amount = (layer2fee.normal as Fee.EthereumFee).amount + requireNotNull(lastLayer1Fee.value),
                gasLimit = layer2fee.normal.gasLimit,
                gasPrice = layer2fee.normal.gasPrice,
            ),
            priority = Fee.EthereumFee(
                amount = (layer2fee.priority as Fee.EthereumFee).amount + requireNotNull(lastLayer1Fee.value),
                gasLimit = layer2fee.priority.gasLimit,
                gasPrice = layer2fee.priority.gasPrice,
            )
        )

        return Result.Success(updatedFees)
    }

    override suspend fun sign(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<Pair<ByteArray, CompiledEthereumTransaction>> {
        //We need to subtract layer 1 fee, because it is deducted automatically
        // and should not be included into transaction for signing
        //https://help.optimism.io/hc/en-us/articles/4411895794715
        val calculatedTransactionFee = (transactionData.fee?.amount?.value ?: BigDecimal.ZERO) -
            (lastLayer1FeeAmount?.value ?: BigDecimal.ZERO)
        val updatedTransactionData = transactionData.copy(
            fee = Fee.CommonFee(Amount(value = calculatedTransactionFee, blockchain = wallet.blockchain))
        )
        return super.sign(updatedTransactionData, signer)
    }

    override suspend fun getFee(amount: Amount, destination: String, data: String): Result<TransactionFee.Choosable> {
        lastLayer1FeeAmount = null

        val blockchain = wallet.blockchain
        val layer2fee = super.getFee(amount, destination, data).successOr {
            return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        }

        val extras = layer2fee.minimum as Fee.EthereumFee

        val preparedAmount = Amount(
            value = BigDecimal.valueOf(0.1),
            blockchain = blockchain
        )

        val gasPriceL2 = extras.gasPrice
        val gasLimitL2 = extras.gasLimit
        val value = preparedAmount.value?.movePointRight(preparedAmount.decimals)?.toBigInteger() ?: BigInteger.ZERO
        val chainId =
            requireNotNull(blockchain.getChainId()) { "${blockchain.fullName} blockchain is not supported by Optimism Wallet Manager" }

        // creating sample transaction that hash should be send to optimism contract to determine layer1Fee
        val txHash = createTransactionWithDefaults(
            from = Address(wallet.address),
            to = Address(destination),
            value = value,
            gasPrice = gasPriceL2,
            gasLimit = gasLimitL2,
            nonce = BigInteger.ONE,
            input = data.removePrefix(HEX_PREFIX).hexToBytes()
        ).encode(SignatureData(v = chainId.toBigInteger()))

        val lastLayer1Fee = getLayer1Fee(txHash).successOr {
            return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        }
        lastLayer1FeeAmount = lastLayer1Fee

        //https://community.optimism.io/docs/developers/build/transaction-fees/#displaying-fees-to-users

        // TODO test this
        val updatedFees = layer2fee.copy(
            minimum = Fee.EthereumFee(
                amount = layer2fee.minimum.amount + requireNotNull(lastLayer1Fee.value),
                gasLimit = layer2fee.minimum.gasLimit,
                gasPrice = layer2fee.minimum.gasPrice
            ),
            normal = Fee.EthereumFee(
                amount = (layer2fee.normal as Fee.EthereumFee).amount + requireNotNull(lastLayer1Fee.value),
                gasLimit = layer2fee.normal.gasLimit,
                gasPrice = layer2fee.normal.gasPrice,
            ),
            priority = Fee.EthereumFee(
                amount = (layer2fee.priority as Fee.EthereumFee).amount + requireNotNull(lastLayer1Fee.value),
                gasLimit = layer2fee.priority.gasLimit,
                gasPrice = layer2fee.priority.gasPrice,
            )
        )

        return Result.Success(updatedFees)
    }

    private suspend fun getLayer1Fee(transactionHash: ByteArray): Result<Amount> {

        return try {
            val transaction = optimism_gas_l1TransactionGenerator(Address(OPTIMISM_FEE_CONTRACT_ADDRESS))
                .getL1Fee(transactionHash)

            val contractCallData = ContractCallData.from(transaction)
            val fee: BigInteger = networkProvider.callContractForFee(contractCallData).successOr {
                return Result.Failure(BlockchainSdkError.FailedToLoadFee)
            }
            val feeIndexed = (fee.toBigDecimal().movePointLeft(wallet.blockchain.decimals()) *
                BigDecimal.valueOf(OPTIMISM_FEE_MULTIPLIER))
            val amount = Amount(
                blockchain = wallet.blockchain,
                value = feeIndexed
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