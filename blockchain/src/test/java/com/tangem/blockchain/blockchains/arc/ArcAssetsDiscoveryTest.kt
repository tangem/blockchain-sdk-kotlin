package com.tangem.blockchain.blockchains.arc

import com.google.common.truth.Truth
import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.assetsdiscovery.providers.evm.DefaultEvmAssetsDiscoveryService
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

internal class ArcAssetsDiscoveryTest {

    private val provider = mockk<EthereumJsonRpcProvider>()
    private val service = DefaultEvmAssetsDiscoveryService(
        multiNetworkProvider = MultiNetworkProvider(providers = listOf(provider), blockchain = Blockchain.Arc),
        blockchain = Blockchain.Arc,
    )

    @Test
    fun `Arc coin balance is discovered via eth_getBalance`() = runTest {
        coEvery { provider.getBalance(WALLET) } returns Result.Success(
            JsonRPCResponse(id = "1", jsonRpc = "2.0", result = "0x1bc16d674ec80000", error = null),
        )

        val assets = service.discoverAssets(WALLET)

        val coin = assets.single() as DiscoveredAsset.Coin
        Truth.assertThat(coin.symbol).isEqualTo("USDC")
        Truth.assertThat(coin.amount.compareTo(BigDecimal("2"))).isEqualTo(0)
    }

    private companion object {
        const val WALLET = "0x6653Db93Cb761Fe63aD959a0B32F0765A9A2EE18"
    }
}