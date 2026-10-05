package com.tangem.blockchain.blockchains.kaspa

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.kaspa.krc20.KaspaKRC20NetworkProvider
import com.tangem.blockchain.blockchains.kaspa.network.KaspaInfoResponse
import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkProvider
import com.tangem.blockchain.blockchains.kaspa.network.KaspaUnspentOutput
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.common.extensions.hexToBytes
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class KaspaWalletManagerUnspentOutputsTest {

    private val networkProvider: KaspaNetworkProvider = mockk(relaxed = true)
    private val publicKey = (
        "04EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FA22C335081AAEBF161095C11C7D8BD550EF88" +
            "82A3125B0EE9AE96DDDE1AE743F"
        ).hexToBytes()
    private val wallet = Wallet(
        blockchain = Blockchain.Kaspa,
        addresses = setOf(
            Address("kaspa:qpsqw2aamda868dlgqczeczd28d5nc3rlrj3t87vu9q58l2tugpjs2psdm4fv", AddressType.Default),
        ),
        publicKey = Wallet.PublicKey(publicKey, null),
        tokens = emptySet(),
    )
    private val transactionBuilder = KaspaTransactionBuilder(publicKey = wallet.publicKey, isTestnet = false)
    private val walletManager = KaspaWalletManager(
        wallet = wallet,
        transactionBuilder = transactionBuilder,
        networkProvider = networkProvider,
        krc20NetworkProvider = mockk<KaspaKRC20NetworkProvider>(relaxed = true),
        dataStorage = mockk<AdvancedDataStorage>(relaxed = true),
        txHistoryProvider = DefaultTransactionHistoryProvider,
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
            KaspaInfoResponse(
                balance = BigDecimal("0.5"),
                unspentOutputs = listOf(
                    utxo(outputIndex = -1, amount = "0.1"),
                    utxo(outputIndex = 4294967296L, amount = "0.1"),
                    utxo(outputIndex = 2, amount = "0"),
                    valid,
                ),
            ),
        )

        // act
        walletManager.updateInternal()

        // assert
        assertThat(transactionBuilder.unspentOutputs).containsExactly(valid)
    }

    private fun utxo(outputIndex: Long, amount: String) = KaspaUnspentOutput(
        amount = BigDecimal(amount),
        outputIndex = outputIndex,
        transactionHash = ByteArray(32) { 0x11 },
        outputScript = ByteArray(34) { 0x22 },
    )
}