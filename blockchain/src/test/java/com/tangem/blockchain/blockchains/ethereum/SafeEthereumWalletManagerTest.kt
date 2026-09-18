package com.tangem.blockchain.blockchains.ethereum

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.network.ContractCallData
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.nft.DefaultNFTProvider
import com.tangem.blockchain.tokenbalance.DefaultTokenBalanceProvider
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.yieldsupply.DefaultYieldSupplyProvider
import io.mockk.coEvery
import io.mockk.mockk
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * The manager reads for the Safe — its wallet address — and never lets an EOA transaction out: every sending and
 * signing entry answers with an unsupported-operation failure before touching the network or the signer.
 */
internal class SafeEthereumWalletManagerTest {

    private val networkProvider: EthereumNetworkProvider = mockk()
    private val signer: TransactionSigner = mockk()

    private val wallet = Wallet(
        blockchain = Blockchain.Ethereum,
        addresses = setOf(Address(SAFE_ADDRESS)),
        publicKey = Wallet.PublicKey(seedKey = ByteArray(size = 65) { 1 }, derivationType = null),
        tokens = emptySet(),
    )

    private val manager = SafeEthereumWalletManager(
        wallet = wallet,
        transactionBuilder = EthereumTransactionBuilder.create(wallet = wallet),
        networkProvider = networkProvider,
        transactionHistoryProvider = DefaultTransactionHistoryProvider,
        nftProvider = DefaultNFTProvider,
        tokenBalanceProvider = DefaultTokenBalanceProvider,
        yieldSupplyProvider = DefaultYieldSupplyProvider,
    )

    @Test
    fun `GIVEN a Safe WHEN getCode THEN the bytecode is read at the Safe address`() = runBlocking {
        coEvery { networkProvider.getCode(address = SAFE_ADDRESS) } returns Result.Success("0x")

        val actual = manager.getCode()

        assertThat((actual as Result.Success).data).isEqualTo("0x")
    }

    @Test
    fun `GIVEN a Safe WHEN getContractNonce THEN the nonce is read at the Safe address`() = runBlocking {
        coEvery { networkProvider.getContractNonce(address = SAFE_ADDRESS) } returns Result.Success(BigInteger.TWO)

        val actual = manager.getContractNonce()

        assertThat((actual as Result.Success).data).isEqualTo(BigInteger.TWO)
    }

    @Test
    fun `GIVEN an inner call WHEN estimateGasLimit THEN the Safe is the caller`() = runBlocking {
        coEvery {
            networkProvider.getGasLimit(to = RECIPIENT, from = SAFE_ADDRESS, value = "0x1", data = "0x")
        } returns Result.Success(BigInteger("21000"))

        val actual = manager.estimateGasLimit(to = RECIPIENT, value = "0x1", data = "0x")

        assertThat((actual as Result.Success).data).isEqualTo(BigInteger("21000"))
    }

    @Test
    fun `GIVEN a Safe method WHEN callSafeContract THEN the call goes to the Safe itself`() = runBlocking {
        coEvery {
            networkProvider.callContract(data = ContractCallData(to = SAFE_ADDRESS, data = GET_THRESHOLD))
        } returns Result.Success("0x2")

        val actual = manager.callSafeContract(data = GET_THRESHOLD)

        assertThat((actual as Result.Success).data).isEqualTo("0x2")
    }

    @Test
    fun `GIVEN a transaction WHEN send THEN it is refused without reaching the network`() = runBlocking {
        val actual = manager.send(transactionData = transaction(), signer = signer, onSigned = {})

        assertRefused(actual)
    }

    @Test
    fun `GIVEN transactions WHEN sendMultiple THEN they are refused`() = runBlocking {
        val actual = manager.sendMultiple(
            transactionDataList = listOf(transaction()),
            signer = signer,
            sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
        )

        assertRefused(actual)
    }

    @Test
    fun `GIVEN a signed transaction WHEN broadcastTransaction THEN it is refused`() = runBlocking {
        val actual = manager.broadcastTransaction(signedTransaction = "0xdeadbeef")

        assertRefused(actual)
    }

    @Test
    fun `GIVEN a transaction WHEN prepareForSend THEN it is refused before signing`() = runBlocking {
        val actual = manager.prepareForSend(transactionData = transaction(), signer = signer)

        assertRefused(actual)
    }

    @Test
    fun `GIVEN a transaction WHEN prepareAndSign THEN it is refused before signing`() = runBlocking {
        val actual = manager.prepareAndSign(transactionData = transaction(), signer = signer)

        assertRefused(actual)
    }

    @Test
    fun `GIVEN transactions WHEN prepareAndSignMultiple THEN they are refused before signing`() = runBlocking {
        val actual = manager.prepareAndSignMultiple(transactionDataList = listOf(transaction()), signer = signer)

        assertRefused(actual)
    }

    private fun assertRefused(actual: Result<*>) {
        assertThat(actual).isInstanceOf(Result.Failure::class.java)
        assertThat((actual as Result.Failure).error).isInstanceOf(BlockchainSdkError.UnsupportedOperation::class.java)
    }

    private fun transaction() = TransactionData.Uncompiled(
        amount = Amount(value = BigDecimal.ONE, blockchain = Blockchain.Ethereum),
        fee = null,
        sourceAddress = SAFE_ADDRESS,
        destinationAddress = RECIPIENT,
    )

    private companion object {
        const val SAFE_ADDRESS = "0xe7411b2aB7b76a11ca0E6174697714983fC09Cb2"
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val GET_THRESHOLD = "0xe75235b8"
    }
}