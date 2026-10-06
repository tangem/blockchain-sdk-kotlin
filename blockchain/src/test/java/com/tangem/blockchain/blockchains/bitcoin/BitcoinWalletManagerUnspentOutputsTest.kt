package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.hexToBytes
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BitcoinWalletManagerUnspentOutputsTest {

    private val networkProvider: BitcoinNetworkProvider = mockk(relaxed = true)
    private val publicKey = (
        "04E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C3356A8681A5456" +
            "18C174251B984DF841F49D2376F"
        ).hexToBytes()
    private val wallet = Wallet(
        blockchain = Blockchain.Bitcoin,
        addresses = setOf(Address("bc1qtest000000000000000000000000000000", AddressType.Default)),
        publicKey = Wallet.PublicKey(publicKey, null),
        tokens = emptySet(),
    )
    private val transactionBuilder = BitcoinTransactionBuilder(publicKey, Blockchain.Bitcoin, wallet.addresses)
    private val walletManager = BitcoinWalletManager(
        wallet = wallet,
        transactionBuilder = transactionBuilder,
        networkProvider = networkProvider,
        feesCalculator = BitcoinFeesCalculator(Blockchain.Bitcoin),
    )

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
    }

    @Test
    fun `out of range outputs from the node are dropped before reaching the builder`() = runTest {
        // arrange
        val valid = utxo(outputIndex = 1, amount = "0.5")
        coEvery { networkProvider.getInfo(any()) } returns Result.Success(
            BitcoinAddressInfo(
                balance = BigDecimal("0.5"),
                unspentOutputs = listOf(
                    utxo(outputIndex = -1, amount = "0.1"),
                    utxo(outputIndex = 4294967296L, amount = "0.1"),
                    utxo(outputIndex = 2, amount = "0"),
                    valid,
                ),
                recentTransactions = emptyList(),
            ),
        )

        // act
        walletManager.updateInternal()

        // assert
        assertThat(transactionBuilder.unspentOutputs).containsExactly(valid)
    }

    private fun utxo(outputIndex: Long, amount: String) = BitcoinUnspentOutput(
        amount = BigDecimal(amount),
        outputIndex = outputIndex,
        transactionHash = ByteArray(32) { 0x11 },
        outputScript = ByteArray(22) { 0x22 },
    )
}