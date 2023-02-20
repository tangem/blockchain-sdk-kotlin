package com.tangem.blockchain.blockchains.optimism

import com.tangem.blockchain.blockchains.ethereum.CompiledEthereumTransaction
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.network.ContractCallData
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import org.kethereum.model.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

class OptimismWalletManager(
    wallet: Wallet,
    transactionBuilder: EthereumTransactionBuilder,
    networkProvider: EthereumNetworkProvider,
    presetTokens: MutableSet<Token>,
) : EthereumWalletManager(wallet, transactionBuilder, networkProvider, presetTokens) {

    private var lastLayer1FeeAmount: Amount? = null

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        lastLayer1FeeAmount = null

        val blockchain = wallet.blockchain
        val layer2fee = super.getFee(amount, destination).successOr {
            return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        }

        val preparedAmount = Amount(
            value = BigDecimal.valueOf(0.1),
            type = amount.type,
            blockchain =  blockchain
        )

        val transactionData = TransactionData(
            amount = preparedAmount,
            fee = preparedAmount,
            sourceAddress = wallet.address,
            destinationAddress = destination,
        )

        val transaction = transactionBuilder.buildToSign(
            transactionData = transactionData,
            nonce = BigInteger.ONE,
            gasLimit = gasLimit
        )?.hash ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)

        val lastLayer1Fee = getLayer1Fee(transaction).successOr {
            return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        }
        lastLayer1FeeAmount = lastLayer1Fee

        //https://community.optimism.io/docs/developers/build/transaction-fees/#displaying-fees-to-users
        val updatedFees = layer2fee.map { it.copy(value = it.value!! + lastLayer1Fee.value!!) }

        return Result.Success(updatedFees)
    }

    override suspend fun sign(
        transactionData: TransactionData,
        signer: TransactionSigner
    ): Result<Pair<ByteArray, CompiledEthereumTransaction>> {
        //We need to subtract layer 1 fee, because it is deducted automatically
        // and should not be included into transaction for signing
        //https://help.optimism.io/hc/en-us/articles/4411895794715
            val calculatedTransactionFee = (transactionData.fee?.value ?: BigDecimal.ZERO) -
                    (lastLayer1FeeAmount?.value ?: BigDecimal.ZERO)
            val updatedTransactionData = transactionData.copy(
                fee = Amount(value = calculatedTransactionFee, blockchain = wallet.blockchain)
            )
        return super.sign(updatedTransactionData, signer)
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

    override fun calculateFees(gasLimit: BigInteger, gasPrice: BigInteger): List<BigDecimal> {
        val minFee = (gasPrice * gasLimit)
        val normalFee = minFee * BigInteger.valueOf(12) / BigInteger.TEN
        val priorityFee = minFee * BigInteger.valueOf(15) / BigInteger.TEN

        val decimals = Blockchain.Ethereum.decimals()
        return listOf(minFee, normalFee, priorityFee)
            .map {
                it.toBigDecimal(
                    scale = decimals,
                    mathContext = MathContext(decimals, RoundingMode.HALF_EVEN)
                )
            }
    }

    companion object {
        private const val OPTIMISM_FEE_CONTRACT_ADDRESS = "0x420000000000000000000000000000000000000F"
        private const val OPTIMISM_FEE_MULTIPLIER = 1.1
    }
}