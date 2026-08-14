package com.tangem.blockchain.blockchains.tron

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.tron.network.BlockHeader
import com.tangem.blockchain.blockchains.tron.network.RawData
import com.tangem.blockchain.blockchains.tron.network.TronBlock
import com.tangem.blockchain.blockchains.tron.network.TronNetworkService
import com.tangem.blockchain.blockchains.tron.tokenmethods.TronTransferTokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.ExtendedSecp256k1Signature
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.common.CompletionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class TronGaslessSigningTest {

    private val tronBlock = TronBlock(
        blockHeader = BlockHeader(
            RawData(
                number = 3111739,
                txTrieRoot = "64288c2db0641316762a99dbb02ef7c90f968b60f9f2e410835980614332f86d",
                witnessAddress = "415863f6091b8e71766da808b1dd3159790f61de7d",
                parentHash = "00000000002f7b3af4f5f8b9e23a30c530f719f165b742e7358536b280eead2d",
                version = 3,
                timestamp = 1539295479000,
            ),
        ),
    )

    private val networkService = mockk<TronNetworkService>(relaxed = true)
    private val wallet = mockk<Wallet>(relaxed = true)
    private val historyProvider = mockk<TransactionHistoryProvider>(relaxed = true)
    private val signer = mockk<TransactionSigner>()

    private val source = "TU1BRXbr6EmKmrLL4Kymv7Wp18eYFkRfAF"
    private val usdt = Token(symbol = "USDT", contractAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t", decimals = 6)

    private val fakePublicKey = Wallet.PublicKey(ByteArray(33) { 0x02 }, null)

    @After
    fun tearDown() {
        unmockkObject(UnmarshalHelper)
    }

    private fun manager(): TronWalletManager {
        every { wallet.address } returns source
        every { wallet.publicKey } returns fakePublicKey
        return TronWalletManager(
            wallet = wallet,
            transactionHistoryProvider = historyProvider,
            transactionBuilder = TronTransactionBuilder(),
            networkService = networkService,
        )
    }

    private fun tokenTransfer(destination: String, value: String): TransactionData.Uncompiled {
        val amount = Amount(token = usdt, value = BigDecimal(value))
        return TransactionData.Uncompiled(
            amount = amount,
            fee = null,
            sourceAddress = source,
            destinationAddress = destination,
            extras = TronTransactionExtras(
                callData = TronTransferTokenCallData(destination = destination, amount = amount),
            ),
        )
    }

    @Test
    fun `GIVEN two token transfers WHEN signGaslessTransactions THEN returns two signed JSONs and never broadcasts`() =
        runTest {
            // Arrange
            coEvery { networkService.getNowBlock() } returns Result.Success(tronBlock)
            val rawSignature = ByteArray(64) { index -> (index + 1).toByte() }
            coEvery { signer.sign(any<List<ByteArray>>(), any()) } returns CompletionResult.Success(
                listOf(rawSignature, rawSignature),
            )
            mockkObject(UnmarshalHelper)
            every {
                UnmarshalHelper.unmarshalSignatureExtended(any(), any(), any<Wallet.PublicKey>())
            } returns ExtendedSecp256k1Signature(
                r = BigInteger.ONE,
                s = BigInteger.TWO,
                recId = 0,
            )
            val compensation = tokenTransfer("TQn9Y2khEsLJW1ChVWFMSMeRDow5KcbLSE", "2.75")
            val original = tokenTransfer("TXXxc9NsHndfQ2z9kMKyWpYa5T3QbhKGwn", "50.0")

            // Act
            val result = manager().signGaslessTransactions(listOf(compensation, original), signer)

            // Assert
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val jsons = (result as Result.Success).data
            assertThat(jsons).hasSize(2)
            val txIdA = JSONObject(jsons[0]).getString("txID")
            val txIdB = JSONObject(jsons[1]).getString("txID")
            assertThat(txIdA).isNotEqualTo(txIdB)
            assertThat(JSONObject(jsons[0]).getJSONArray("signature").length()).isEqualTo(1)
            coVerify(exactly = 0) { networkService.broadcastHex(any()) }
            coVerify(exactly = 1) { signer.sign(match<List<ByteArray>> { it.size == 2 }, any()) }
        }
}