package com.tangem.blockchain.blockchains.ton

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ton.models.TonPreSignStructure
import com.tangem.blockchain.blockchains.ton.models.TonWalletInfo
import com.tangem.blockchain.blockchains.ton.network.TonAccountState
import com.tangem.blockchain.blockchains.ton.network.TonNetworkProvider
import com.tangem.blockchain.blockchains.ton.network.TonNetworkService
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.InitializableAccount
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
internal class TonWalletManagerTest {

    private val token = Token(name = "Jetton", symbol = "JET", contractAddress = "EQminter", decimals = 6)
    private val signer = mockk<TransactionSigner>()

    // byte array whose lowercase hex is "1234abcd" — see assertions below
    private val hashBytes = byteArrayOf(0x12, 0x34, 0xAB.toByte(), 0xCD.toByte())
    private val base64Hash = Base64.encode(hashBytes)
    private val expectedHex = "1234abcd"

    private lateinit var wallet: Wallet
    private lateinit var walletManager: TonWalletManager

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.d(any(), any()) } returns 0

        mockkConstructor(TonNetworkService::class)
        mockkConstructor(TonTransactionBuilder::class)
        every { anyConstructed<TonNetworkService>().host } returns "https://ton.test"
        every { anyConstructed<TonTransactionBuilder>().updateJettonAdresses(any()) } just Runs

        wallet = Wallet(
            blockchain = Blockchain.TON,
            addresses = setOf(Address("EQsender", AddressType.Default)),
            publicKey = mockk(relaxed = true),
            tokens = emptySet(),
        )
        walletManager = TonWalletManager(
            wallet = wallet,
            networkProviders = listOf(mockk<TonNetworkProvider>(relaxed = true)),
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // region updateInternal

    @Test
    fun `updateInternal marks account INITIALIZED and confirms recent transactions on new sequence number`() = runTest {
        seedRecentTransaction()
        val info = TonWalletInfo(
            accountState = TonAccountState.ACTIVE,
            balance = BigDecimal("5"),
            sequenceNumber = 7, // differs from initial sequenceNumber (0) -> recent txs become confirmed
            jettonDatas = emptyMap(),
        )
        coEvery {
            anyConstructed<TonNetworkService>().getWalletInformation(wallet.address, any())
        } returns Result.Success(info)

        walletManager.updateInternal()

        assertThat(walletManager.accountInitializationState).isEqualTo(InitializableAccount.State.INITIALIZED)
        assertThat(wallet.amounts[AmountType.Coin]?.value?.compareTo(BigDecimal("5"))).isEqualTo(0)
        assertThat(wallet.recentTransactions.single().status).isEqualTo(TransactionStatus.Confirmed)
    }

    @Test
    fun `updateInternal marks account NOT_INITIALIZED and keeps recent transactions on same sequence number`() =
        runTest {
            seedRecentTransaction()
            val info = TonWalletInfo(
                accountState = TonAccountState.UNINITIALIZED,
                balance = BigDecimal.ZERO,
                sequenceNumber = 0, // equals initial sequenceNumber (0) -> recent txs stay unconfirmed
                jettonDatas = emptyMap(),
            )
            coEvery {
                anyConstructed<TonNetworkService>().getWalletInformation(wallet.address, any())
            } returns Result.Success(info)

            walletManager.updateInternal()

            assertThat(walletManager.accountInitializationState).isEqualTo(InitializableAccount.State.NOT_INITIALIZED)
            assertThat(wallet.recentTransactions.single().status).isEqualTo(TransactionStatus.Unconfirmed)
        }

    @Test
    fun `updateInternal rethrows BlockchainSdkError when wallet information fails`() = runTest {
        coEvery {
            anyConstructed<TonNetworkService>().getWalletInformation(wallet.address, any())
        } returns Result.Failure(BlockchainSdkError.AccountNotFound())

        var error: BlockchainSdkError? = null
        try {
            walletManager.updateInternal()
        } catch (e: BlockchainSdkError) {
            error = e
        }

        assertThat(error).isInstanceOf(BlockchainSdkError.AccountNotFound::class.java)
    }

    // endregion

    // region send

    @Test
    fun `send compiled transaction returns lowercase hex hash`() = runTest {
        stubBuildAndSign()
        coEvery { anyConstructed<TonNetworkService>().send("boc") } returns Result.Success(base64Hash)

        val result = walletManager.send(compiledTransaction(), signer)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data.hash).isEqualTo(expectedHex)
    }

    @Test
    fun `send uncompiled transaction returns hex hash and records outgoing transaction`() = runTest {
        stubBuildAndSign()
        coEvery { anyConstructed<TonNetworkService>().send("boc") } returns Result.Success(base64Hash)

        val result = walletManager.send(uncompiledTransaction(), signer)

        assertThat((result as Result.Success).data.hash).isEqualTo(expectedHex)
        assertThat(wallet.recentTransactions.single().hash).isEqualTo(expectedHex)
    }

    @Test
    fun `send returns failure when network send fails`() = runTest {
        stubBuildAndSign()
        coEvery {
            anyConstructed<TonNetworkService>().send(any())
        } returns Result.Failure(BlockchainSdkError.FailedToSendException)

        val result = walletManager.send(uncompiledTransaction(), signer)

        assertThat(result).isInstanceOf(Result.Failure::class.java)
        assertThat((result as Result.Failure).error).isEqualTo(BlockchainSdkError.FailedToSendException)
        assertThat(wallet.recentTransactions).isEmpty()
    }

    @Test
    fun `send wraps BlockchainSdkError thrown while building transaction`() = runTest {
        every {
            anyConstructed<TonTransactionBuilder>().buildForSign(any(), any(), any(), any(), any())
        } throws BlockchainSdkError.FailedToBuildTx

        val result = walletManager.send(uncompiledTransaction(), signer)

        assertThat(result).isInstanceOf(Result.Failure::class.java)
        assertThat((result as Result.Failure).error).isEqualTo(BlockchainSdkError.FailedToBuildTx)
    }

    @Test
    fun `send wraps generic exception thrown while building transaction`() = runTest {
        every {
            anyConstructed<TonTransactionBuilder>().buildForSign(any(), any(), any(), any(), any())
        } throws IllegalStateException("boom")

        val result = walletManager.send(uncompiledTransaction(), signer)

        assertThat(result).isInstanceOf(Result.Failure::class.java)
        assertThat((result as Result.Failure).error).isInstanceOf(BlockchainSdkError::class.java)
    }

    // endregion

    // region getFee

    @Test
    fun `getFee returns single fee for coin amount`() = runTest {
        stubFeeMessage()
        coEvery {
            anyConstructed<TonNetworkService>().getFee(wallet.address, "boc")
        } returns Result.Success(Amount(BigDecimal("0.01"), Blockchain.TON))

        val result = walletManager.getFee(Amount(BigDecimal.ONE, Blockchain.TON), "EQdest")

        val fee = ((result as Result.Success).data as TransactionFee.Single).normal
        assertThat(fee.amount.value?.compareTo(BigDecimal("0.01"))).isEqualTo(0)
    }

    @Test
    fun `getFee adds inactive jetton processing fee for token amount`() = runTest {
        stubFeeMessage()
        coEvery {
            anyConstructed<TonNetworkService>().getFee(any(), any())
        } returns Result.Success(Amount(BigDecimal("0.01"), Blockchain.TON))
        coEvery { anyConstructed<TonNetworkService>().getJettonWalletAddress(any()) } returns Result.Success("EQjw")
        coEvery { anyConstructed<TonNetworkService>().isJettonWalletActive("EQjw") } returns Result.Success(false)

        val result = walletManager.getFee(Amount(token, BigDecimal.ONE), "EQdest")

        val fee = ((result as Result.Success).data as TransactionFee.Single).normal
        // 0.01 (network) + 0.05 (JETTON_TRANSFER_PROCESSING_FEE)
        assertThat(fee.amount.value?.compareTo(BigDecimal("0.06"))).isEqualTo(0)
    }

    @Test
    fun `getFee adds active jetton processing fee for token amount`() = runTest {
        stubFeeMessage()
        coEvery {
            anyConstructed<TonNetworkService>().getFee(any(), any())
        } returns Result.Success(Amount(BigDecimal("0.01"), Blockchain.TON))
        coEvery { anyConstructed<TonNetworkService>().getJettonWalletAddress(any()) } returns Result.Success("EQjw")
        coEvery { anyConstructed<TonNetworkService>().isJettonWalletActive("EQjw") } returns Result.Success(true)

        val result = walletManager.getFee(Amount(token, BigDecimal.ONE), "EQdest")

        val fee = ((result as Result.Success).data as TransactionFee.Single).normal
        // 0.01 (network) + 0.0001 (JETTON_TRANSFER_PROCESSING_FEE_ACTIVE_WALLET)
        assertThat(fee.amount.value?.compareTo(BigDecimal("0.0101"))).isEqualTo(0)
    }

    @Test
    fun `getFee returns failure when network fee request fails`() = runTest {
        stubFeeMessage()
        coEvery {
            anyConstructed<TonNetworkService>().getFee(any(), any())
        } returns Result.Failure(BlockchainSdkError.FailedToLoadFee)

        val result = walletManager.getFee(Amount(BigDecimal.ONE, Blockchain.TON), "EQdest")

        assertThat(result).isInstanceOf(Result.Failure::class.java)
        assertThat((result as Result.Failure).error).isEqualTo(BlockchainSdkError.FailedToLoadFee)
    }

    @Test
    fun `getFee wraps BlockchainSdkError thrown while building message`() = runTest {
        every {
            anyConstructed<TonTransactionBuilder>().buildForSend(any(), any(), any(), any(), any(), any())
        } throws BlockchainSdkError.FailedToBuildTx

        val result = walletManager.getFee(Amount(BigDecimal.ONE, Blockchain.TON), "EQdest")

        assertThat(result).isInstanceOf(Result.Failure::class.java)
        assertThat((result as Result.Failure).error).isEqualTo(BlockchainSdkError.FailedToBuildTx)
    }

    @Test
    fun `getFee wraps generic exception thrown while building message`() = runTest {
        every {
            anyConstructed<TonTransactionBuilder>().buildForSend(any(), any(), any(), any(), any(), any())
        } throws IllegalStateException("boom")

        val result = walletManager.getFee(Amount(BigDecimal.ONE, Blockchain.TON), "EQdest")

        assertThat(result).isInstanceOf(Result.Failure::class.java)
        assertThat((result as Result.Failure).error).isInstanceOf(BlockchainSdkError::class.java)
    }

    // endregion

    private fun stubBuildAndSign() {
        val preSign = TonPreSignStructure(hashToSign = byteArrayOf(1, 2, 3), inputData = byteArrayOf(4, 5, 6))
        every {
            anyConstructed<TonTransactionBuilder>().buildForSign(any(), any(), any(), any(), any())
        } returns preSign
        every {
            anyConstructed<TonTransactionBuilder>().buildCompiledForSign(any(), any())
        } returns preSign
        coEvery { signer.sign(any<ByteArray>(), any()) } returns CompletionResult.Success(ByteArray(64))
        every {
            anyConstructed<TonTransactionBuilder>().buildForSend(any<ByteArray>(), any<TonPreSignStructure>())
        } returns "boc"
    }

    private fun stubFeeMessage() {
        every {
            anyConstructed<TonTransactionBuilder>().buildForSend(any(), any(), any(), any(), any(), any())
        } returns "boc"
    }

    private fun seedRecentTransaction() {
        wallet.addOutgoingTransaction(uncompiledTransaction(), txHash = "seedhash")
    }

    private fun uncompiledTransaction(): TransactionData.Uncompiled = TransactionData.Uncompiled(
        amount = Amount(BigDecimal.ONE, Blockchain.TON),
        fee = Fee.Common(Amount(BigDecimal("0.01"), Blockchain.TON)),
        sourceAddress = wallet.address,
        destinationAddress = "EQdest",
    )

    private fun compiledTransaction(): TransactionData.Compiled = TransactionData.Compiled(
        value = TransactionData.Compiled.Data.Bytes(byteArrayOf(9, 8, 7)),
    )
}