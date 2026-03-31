package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BitcoinConsolidationTest {

    private val blockchain = Blockchain.Bitcoin
    private val baseAddress = "bc1qtest000000000000000000000000000000"
    private lateinit var walletManager: BitcoinWalletManager

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0

        val walletPublicKey = ByteArray(33) { 0x02 }
        val wallet = Wallet(
            blockchain = blockchain,
            addresses = setOf(Address(baseAddress, AddressType.Default)),
            publicKey = Wallet.PublicKey(walletPublicKey, null),
            tokens = emptySet(),
        )
        val networkProvider: BitcoinNetworkProvider = mockk(relaxed = true)
        walletManager = BitcoinWalletManager(
            wallet = wallet,
            transactionBuilder = BitcoinTransactionBuilder(walletPublicKey, blockchain, wallet.addresses),
            networkProvider = networkProvider,
            feesCalculator = BitcoinFeesCalculator(blockchain),
        )
    }

    @Test
    fun createConsolidation_allUtxosToBaseAddress() {
        val utxos = createTestUtxos(listOf("0.001", "0.002", "0.003"))
        setUtxos(utxos)

        val fee = Fee.Common(Amount(BigDecimal("0.0001"), blockchain, AmountType.Coin))
        val result = walletManager.createConsolidationTransaction(fee)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        val txData = (result as Result.Success).data
        assertThat(txData.destinationAddress).isEqualTo(baseAddress)
        assertThat(txData.sourceAddress).isEqualTo(baseAddress)
    }

    @Test
    fun createConsolidation_amountIsTotalMinusFee() {
        val utxos = createTestUtxos(listOf("0.001", "0.002", "0.003"))
        setUtxos(utxos)

        val feeValue = BigDecimal("0.0001")
        val fee = Fee.Common(Amount(feeValue, blockchain, AmountType.Coin))
        val result = walletManager.createConsolidationTransaction(fee)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        val txData = (result as Result.Success).data

        val expectedAmount = BigDecimal("0.006") - feeValue
        assertThat(txData.amount.value).isEqualTo(expectedAmount)
    }

    @Test
    fun createConsolidation_balanceLessThanFee_error() {
        val utxos = createTestUtxos(listOf("0.00001"))
        setUtxos(utxos)

        val fee = Fee.Common(Amount(BigDecimal("0.001"), blockchain, AmountType.Coin))
        val result = walletManager.createConsolidationTransaction(fee)

        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun createConsolidation_noUtxos_error() {
        // Don't set any UTXOs — walletManager starts with null/empty UTXOs
        val fee = Fee.Common(Amount(BigDecimal("0.0001"), blockchain, AmountType.Coin))
        val result = walletManager.createConsolidationTransaction(fee)

        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    private fun setUtxos(utxos: List<BitcoinUnspentOutput>) {
        val balance = utxos.sumOf { it.amount }
        walletManager.updateWallet(
            com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo(
                balance = balance,
                unspentOutputs = utxos,
                recentTransactions = emptyList(),
            ),
        )
    }

    private fun createTestUtxos(amounts: List<String>): List<BitcoinUnspentOutput> {
        return amounts.mapIndexed { i, amount ->
            BitcoinUnspentOutput(
                amount = amount.toBigDecimal(),
                outputIndex = i.toLong(),
                transactionHash = ByteArray(32) { (i + 1).toByte() },
                outputScript = ByteArray(25) { 0x00 }, // dummy script
                address = "bc1qaddr$i",
                derivationPath = "m/84'/0'/0'/0/$i",
            )
        }
    }
}