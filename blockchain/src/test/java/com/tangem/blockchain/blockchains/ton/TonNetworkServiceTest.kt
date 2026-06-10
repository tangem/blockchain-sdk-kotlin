package com.tangem.blockchain.blockchains.ton

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ton.network.TonAccountState
import com.tangem.blockchain.blockchains.ton.network.TonGetWalletInfoResponse
import com.tangem.blockchain.blockchains.ton.network.TonNetworkProvider
import com.tangem.blockchain.blockchains.ton.network.TonNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class TonNetworkServiceTest {

    private val ownerAddress = "EQowner"

    private val goodToken = Token(name = "Good", symbol = "GOOD", contractAddress = "GOOD_MINTER", decimals = 6)
    private val badToken = Token(name = "Bad", symbol = "BAD", contractAddress = "BAD_MINTER", decimals = 6)

    private val coinInfo = TonGetWalletInfoResponse(
        wallet = true,
        balance = BigDecimal("5000000000"), // 5 TON (9 decimals)
        accountState = TonAccountState.ACTIVE,
        seqno = 7,
    )

    // The exit-code-11 / non-Jetton case surfaces from getJettonWalletAddress as a WrappedThrowable
    // (the requireNotNull parse failure), which is a non-network error returned immediately by MultiNetworkProvider.
    private val parseFailure = Result.Failure(
        BlockchainSdkError.WrappedThrowable(IllegalArgumentException("Can not parse response")),
    )

    private fun serviceWith(provider: TonNetworkProvider): TonNetworkService {
        every { provider.baseUrl } returns "https://ton.test"
        return TonNetworkService(jsonRpcProviders = listOf(provider), blockchain = Blockchain.TON)
    }

    @Test
    fun `drops token whose jetton wallet address cannot be resolved and keeps the rest`() = runTest {
        val provider = mockk<TonNetworkProvider>()
        coEvery { provider.getWalletInformation(ownerAddress) } returns Result.Success(coinInfo)
        coEvery {
            provider.getJettonWalletAddress(match { it.jettonMinterAddress == badToken.contractAddress })
        } returns parseFailure
        coEvery {
            provider.getJettonWalletAddress(match { it.jettonMinterAddress == goodToken.contractAddress })
        } returns Result.Success("EQgoodJettonWallet")
        coEvery { provider.getJettonBalance("EQgoodJettonWallet") } returns Result.Success(BigInteger("2000000"))

        val result = serviceWith(provider).getWalletInformation(ownerAddress, setOf(goodToken, badToken))

        assertThat(result).isInstanceOf(Result.Success::class.java)
        val info = (result as Result.Success).data
        assertThat(info.balance.compareTo(BigDecimal("5"))).isEqualTo(0) // coin balance survives
        assertThat(info.jettonDatas.keys).containsExactly(goodToken) // bad token dropped
        assertThat(info.jettonDatas.getValue(goodToken).balance.compareTo(BigDecimal("2"))).isEqualTo(0)
    }

    @Test
    fun `drops token whose balance fetch fails and keeps the rest`() = runTest {
        val provider = mockk<TonNetworkProvider>()
        coEvery { provider.getWalletInformation(ownerAddress) } returns Result.Success(coinInfo)
        coEvery {
            provider.getJettonWalletAddress(match { it.jettonMinterAddress == goodToken.contractAddress })
        } returns Result.Success("EQgoodJettonWallet")
        coEvery {
            provider.getJettonWalletAddress(match { it.jettonMinterAddress == badToken.contractAddress })
        } returns Result.Success("EQbadJettonWallet")
        coEvery { provider.getJettonBalance("EQgoodJettonWallet") } returns Result.Success(BigInteger("2000000"))
        coEvery { provider.getJettonBalance("EQbadJettonWallet") } returns parseFailure

        val result = serviceWith(provider).getWalletInformation(ownerAddress, setOf(goodToken, badToken))

        assertThat(result).isInstanceOf(Result.Success::class.java)
        val info = (result as Result.Success).data
        assertThat(info.jettonDatas.keys).containsExactly(goodToken)
    }

    @Test
    fun `fails whole update when coin balance cannot be read`() = runTest {
        val provider = mockk<TonNetworkProvider>()
        coEvery { provider.getWalletInformation(ownerAddress) } returns parseFailure
        coEvery { provider.getJettonWalletAddress(any()) } returns Result.Success("EQgoodJettonWallet")
        coEvery { provider.getJettonBalance(any()) } returns Result.Success(BigInteger("2000000"))

        val result = serviceWith(provider).getWalletInformation(ownerAddress, setOf(goodToken))

        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }
}