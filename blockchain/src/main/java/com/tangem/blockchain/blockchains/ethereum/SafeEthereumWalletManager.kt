package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.network.ContractCallData
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.common.transaction.TransactionsSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.nft.NFTProvider
import com.tangem.blockchain.tokenbalance.TokenBalanceProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider
import java.math.BigInteger

/**
 * The manager of a Safe the wallet co-owns. The wallet's key is real — it signs the Safe's operations — but the
 * wallet's address is the Safe's, so balances, history and fee estimates are read for the Safe like for any account.
 *
 * The Safe never sends an EOA transaction: an operation is executed by the relayer once the co-owners have signed
 * it. Every sending and signing entry of [EthereumWalletManager] is therefore refused rather than left to sign an
 * EOA transaction of the owner key against the Safe's nonce.
 *
[REDACTED_AUTHOR]
 */
class SafeEthereumWalletManager @Suppress("LongParameterList") internal constructor(
    wallet: Wallet,
    transactionBuilder: EthereumTransactionBuilder,
    networkProvider: EthereumNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider,
    nftProvider: NFTProvider,
    tokenBalanceProvider: TokenBalanceProvider,
    yieldSupplyProvider: YieldSupplyProvider,
) : EthereumWalletManager(
    wallet = wallet,
    transactionBuilder = transactionBuilder,
    networkProvider = networkProvider,
    transactionHistoryProvider = transactionHistoryProvider,
    nftProvider = nftProvider,
    tokenBalanceProvider = tokenBalanceProvider,
    supportsENS = false,
    yieldSupplyProvider = yieldSupplyProvider,
) {

    /** The bytecode at the Safe's address: `0x` until the first operation deploys the proxy */
    suspend fun getCode(): Result<String> = networkProvider.getCode(address = wallet.address)

    /** The Safe's own `nonce()`, the next operation's nonce; unavailable while the Safe is not deployed */
    suspend fun getContractNonce(): Result<BigInteger> = networkProvider.getContractNonce(address = wallet.address)

    /** `eth_estimateGas` of a call made by the Safe, the inner call of an operation */
    suspend fun estimateGasLimit(to: String, value: String?, data: String?): Result<BigInteger> {
        return networkProvider.getGasLimit(to = to, from = wallet.address, value = value, data = data)
    }

    /** A read-only call to the Safe contract itself: `getOwners()`, `getThreshold()` and the like */
    suspend fun callSafeContract(data: String): Result<String> {
        return networkProvider.callContract(data = ContractCallData(to = wallet.address, data = data))
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
        onSigned: () -> Unit,
    ): Result<TransactionSendResult> = refused()

    override suspend fun sendMultiple(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
        sendMode: TransactionSender.MultipleTransactionSendMode,
    ): Result<TransactionsSendResult> = refused()

    override suspend fun broadcastTransaction(signedTransaction: String): Result<TransactionSendResult> = refused()

    override suspend fun prepareForSend(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<ByteArray> {
        return refused()
    }

    override suspend fun prepareAndSign(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<ByteArray> {
        return refused()
    }

    override suspend fun prepareAndSignMultiple(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
    ): Result<List<ByteArray>> = refused()

    private fun <T> refused(): Result<T> = Result.Failure(BlockchainSdkError.UnsupportedOperation(SEND_REFUSED))

    private companion object {
        const val SEND_REFUSED = "A Safe sends nothing itself: its operations are executed by the relayer"
    }
}