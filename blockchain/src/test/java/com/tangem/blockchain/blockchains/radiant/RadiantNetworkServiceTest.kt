package com.tangem.blockchain.blockchains.radiant

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.radiant.network.RadiantNetworkService
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.electrum.ElectrumAccount
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import com.tangem.blockchain.network.electrum.ElectrumUnspentUTXORecord
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class RadiantNetworkServiceTest {

    private val provider: ElectrumNetworkProvider = mockk(relaxed = true)
    private val service = RadiantNetworkService(listOf(provider))

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
    }

    @Test
    fun `out of range outputs from the node are dropped from account info`() = runTest {
        // arrange
        val valid = utxo(txPos = 1, value = "0.5")
        coEvery { provider.getAccountBalance(any()) } returns Result.Success(
            ElectrumAccount(confirmedAmount = BigDecimal("0.5"), unconfirmedAmount = BigDecimal.ZERO),
        )
        coEvery { provider.getUnspentUTXOs(any()) } returns Result.Success(
            listOf(
                utxo(txPos = -1, value = "0.1"),
                utxo(txPos = 4294967296L, value = "0.1"),
                utxo(txPos = 2, value = "0"),
                valid,
            ),
        )

        // act
        val result = service.getInfo(scriptHash = "script-hash")

        // assert
        assertThat((result as Result.Success).data.unspentOutputs).containsExactly(valid)
    }

    private fun utxo(txPos: Long, value: String) = ElectrumUnspentUTXORecord(
        height = 100,
        txPos = txPos,
        txHash = "11".repeat(32),
        value = BigDecimal(value),
        outpointHash = null,
    )
}