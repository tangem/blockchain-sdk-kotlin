package com.tangem.blockchain.pendingtransactions

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.datastorage.PendingTransaction
import com.tangem.blockchain.common.logging.SensitiveKeys
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.pendingtransactions.providers.EthereumPendingTransactionsProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

internal class EthereumPendingTransactionsProviderTest {

    private val blink = provider(url = "https://eth.blinklabs.xyz/v1/")
    private val publicNode = provider(url = PUBLIC_NODE_URL)
    private val nowNodes = provider(url = "https://eth.nownodes.io/")

    private val networkProviderMap: Map<ProviderType, NetworkProvider> = mapOf(
        ProviderType.Blink to blink,
        ProviderType.Public(PUBLIC_NODE_URL) to publicNode,
        ProviderType.NowNodes to nowNodes,
    )

    private val storage: PendingTransactionStorage = mockk()
    private val wallet: Wallet = mockk { every { blockchain } returns Blockchain.Ethereum }

    private val pendingTransactionsProvider = EthereumPendingTransactionsProvider(
        wallet = wallet,
        multiJsonRpcProvider = MultiNetworkProvider(
            providers = listOf(blink, publicNode, nowNodes),
            blockchain = Blockchain.Ethereum,
        ),
        storage = storage,
        networkProviderMap = networkProviderMap,
    )

    @Before
    fun setUp() {
        mockkObject(SensitiveKeys)
        every { SensitiveKeys.data } returns emptyList()
        coEvery { storage.addTransaction(any(), any(), any()) } just runs
        coEvery { storage.removeTransactions(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkObject(SensitiveKeys)
    }

    @Test
    fun `GIVEN gasless transaction WHEN added THEN no provider is saved`() = runTest {
        // Act
        pendingTransactionsProvider.addPendingGaslessTransaction(TX_HASH, mockk(relaxed = true))

        // Assert
        coVerify(exactly = 1) { storage.addTransaction(TX_HASH, null, any()) }
    }

    @Test
    fun `GIVEN transaction sent through public node WHEN added THEN no provider is saved`() = runTest {
        // Act
        pendingTransactionsProvider.addPendingTransaction(TX_HASH, publicNode, mockk(relaxed = true))

        // Assert
        coVerify(exactly = 1) { storage.addTransaction(TX_HASH, null, any()) }
    }

    @Test
    fun `GIVEN transaction sent through Blink WHEN added THEN Blink is saved`() = runTest {
        // Act
        pendingTransactionsProvider.addPendingTransaction(TX_HASH, blink, mockk(relaxed = true))

        // Assert
        coVerify(exactly = 1) { storage.addTransaction(TX_HASH, BLINK_KEY, any()) }
    }

    @Test
    fun `GIVEN transaction without provider WHEN first node fails THEN next node is asked and Blink is not`() =
        runTest {
            // Arrange
            givenStoredTransaction(providerName = null)
            coEvery { publicNode.getTransactionByHash(TX_HASH) } returns networkFailure()
            coEvery { nowNodes.getTransactionByHash(TX_HASH) } returns transactionFound(blockNumber = "0x1")

            // Act
            val statuses = pendingTransactionsProvider.checkPendingTransactions()

            // Assert
            assertThat(statuses).containsExactly(TX_HASH, PendingTransactionStatus.Executed)
            coVerify(exactly = 1) { storage.removeTransactions(listOf(TX_HASH)) }
            coVerify(exactly = 0) { blink.getTransactionByHash(any()) }
        }

    @Test
    fun `GIVEN record pinned to public node by older version WHEN that node fails THEN other nodes are asked`() =
        runTest {
            // Arrange
            givenStoredTransaction(providerName = PUBLIC_NODE_KEY)
            coEvery { publicNode.getTransactionByHash(TX_HASH) } returns networkFailure()
            coEvery { nowNodes.getTransactionByHash(TX_HASH) } returns transactionFound(blockNumber = "0x1")

            // Act
            val statuses = pendingTransactionsProvider.checkPendingTransactions()

            // Assert
            assertThat(statuses).containsExactly(TX_HASH, PendingTransactionStatus.Executed)
        }

    @Test
    fun `GIVEN transaction without provider WHEN all nodes fail THEN stays pending`() = runTest {
        // Arrange
        givenStoredTransaction(providerName = null, sentAt = LONG_AGO)
        coEvery { publicNode.getTransactionByHash(TX_HASH) } returns networkFailure()
        coEvery { nowNodes.getTransactionByHash(TX_HASH) } returns networkFailure()

        // Act
        val statuses = pendingTransactionsProvider.checkPendingTransactions()

        // Assert
        assertThat(statuses).containsExactly(TX_HASH, PendingTransactionStatus.Pending)
        coVerify(exactly = 0) { storage.removeTransactions(any()) }
    }

    @Test
    fun `GIVEN all nodes return json rpc error WHEN check THEN stays pending instead of dropped`() = runTest {
        // Arrange
        givenStoredTransaction(providerName = null, sentAt = LONG_AGO)
        coEvery { publicNode.getTransactionByHash(TX_HASH) } returns rpcError()
        coEvery { nowNodes.getTransactionByHash(TX_HASH) } returns rpcError()

        // Act
        val statuses = pendingTransactionsProvider.checkPendingTransactions()

        // Assert
        assertThat(statuses).containsExactly(TX_HASH, PendingTransactionStatus.Pending)
    }

    @Test
    fun `GIVEN transaction without provider WHEN node does not know it THEN dropped`() = runTest {
        // Arrange
        givenStoredTransaction(providerName = null)
        coEvery { publicNode.getTransactionByHash(TX_HASH) } returns transactionNotFound()

        // Act
        val statuses = pendingTransactionsProvider.checkPendingTransactions()

        // Assert
        assertThat(statuses).containsExactly(TX_HASH, PendingTransactionStatus.Dropped)
    }

    @Test
    fun `GIVEN transaction pinned to Blink WHEN Blink does not know it THEN dropped as before`() = runTest {
        // Arrange
        givenStoredTransaction(providerName = BLINK_KEY)
        coEvery { blink.getTransactionByHash(TX_HASH) } returns transactionNotFound()

        // Act
        val statuses = pendingTransactionsProvider.checkPendingTransactions()

        // Assert
        assertThat(statuses).containsExactly(TX_HASH, PendingTransactionStatus.Dropped)
        coVerify(exactly = 0) { nowNodes.getTransactionByHash(any()) }
    }

    @Test
    fun `GIVEN transaction pinned to Blink WHEN Blink fails THEN fallback finds it`() = runTest {
        // Arrange
        givenStoredTransaction(providerName = BLINK_KEY)
        coEvery { blink.getTransactionByHash(TX_HASH) } returns networkFailure()
        coEvery { publicNode.getTransactionByHash(TX_HASH) } returns transactionFound(blockNumber = "0x1")

        // Act
        val statuses = pendingTransactionsProvider.checkPendingTransactions()

        // Assert
        assertThat(statuses).containsExactly(TX_HASH, PendingTransactionStatus.Executed)
    }

    private fun givenStoredTransaction(providerName: String?, sentAt: Long = System.currentTimeMillis()) {
        coEvery { storage.getTransactions() } returns listOf(
            PendingTransaction(
                transactionId = TX_HASH,
                blockchain = Blockchain.Ethereum.id,
                providerName = providerName,
                sentAt = sentAt,
            ),
        )
    }

    private fun provider(url: String) = mockk<EthereumJsonRpcProvider> {
        every { baseUrl } returns url
    }

    private fun networkFailure() = Result.Failure(BlockchainSdkError.WrappedThrowable(IOException("boom")))

    private fun transactionFound(blockNumber: String?) = Result.Success(
        JsonRPCResponse(
            id = "1",
            jsonRpc = "2.0",
            result = mapOf(
                "blockHash" to blockNumber?.let { "0xblock" },
                "blockNumber" to blockNumber,
                "from" to "0x1111111111111111111111111111111111111111",
                "gas" to "0x5208",
                "gasPrice" to "0x1",
                "hash" to TX_HASH,
                "input" to "0x",
                "nonce" to "0x0",
                "to" to "0x2222222222222222222222222222222222222222",
                "transactionIndex" to blockNumber?.let { "0x0" },
                "value" to "0x0",
            ),
            error = null,
        ),
    )

    private fun transactionNotFound() = Result.Success(
        JsonRPCResponse(id = "1", jsonRpc = "2.0", result = null, error = null),
    )

    private fun rpcError() = Result.Success(
        JsonRPCResponse(
            id = "1",
            jsonRpc = "2.0",
            result = null,
            error = JsonRPCResponse.Error(code = -32005, message = "limit exceeded"),
        ),
    )

    private companion object {
        const val TX_HASH = "0xaa78dcd2969528a33b3ec0d3a9ed3418448d0f09c5e89006bf42376d5f0f29a5"
        const val PUBLIC_NODE_URL = "https://ethereum-rpc.publicnode.com/"
        const val PUBLIC_NODE_KEY = "Public:$PUBLIC_NODE_URL"
        const val BLINK_KEY = "com.tangem.blockchain.common.network.providers.ProviderType.Blink"
        const val LONG_AGO = 0L
    }
}