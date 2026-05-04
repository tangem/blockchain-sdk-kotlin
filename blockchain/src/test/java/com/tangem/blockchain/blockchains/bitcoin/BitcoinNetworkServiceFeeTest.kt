package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.bitcoin.network.XpubInfoResponse
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

internal class BitcoinNetworkServiceFeeTest {

    @Test
    fun `getFee with single provider returns its fee`() = runTest {
        val fee = BitcoinFee(
            minimalPerKb = BigDecimal("0.00010000"),
            normalPerKb = BigDecimal("0.00020000"),
            priorityPerKb = BigDecimal("0.00030000"),
        )
        val service = createService(fee)

        val result = service.getFee() as Result.Success

        assertThat(result.data).isEqualTo(fee)
    }

    @Test
    fun `getFee with two providers returns arithmetic mean`() = runTest {
        val fee1 = BitcoinFee(
            minimalPerKb = BigDecimal("0.00010000"),
            normalPerKb = BigDecimal("0.00020000"),
            priorityPerKb = BigDecimal("0.00040000"),
        )
        val fee2 = BitcoinFee(
            minimalPerKb = BigDecimal("0.00020000"),
            normalPerKb = BigDecimal("0.00030000"),
            priorityPerKb = BigDecimal("0.00060000"),
        )
        val service = createService(fee1, fee2)

        val result = service.getFee() as Result.Success

        assertThat(result.data).isEqualTo(
            BitcoinFee(
                minimalPerKb = BigDecimal("0.00015000"),
                normalPerKb = BigDecimal("0.00025000"),
                priorityPerKb = BigDecimal("0.00050000"),
            ),
        )
    }

    @Test
    fun `getFee with three providers returns trimmed mean`() = runTest {
        val fee1 = BitcoinFee(
            minimalPerKb = BigDecimal("0.00010000"),
            normalPerKb = BigDecimal("0.00020000"),
            priorityPerKb = BigDecimal("0.00030000"),
        )
        val fee2 = BitcoinFee(
            minimalPerKb = BigDecimal("0.00050000"),
            normalPerKb = BigDecimal("0.00060000"),
            priorityPerKb = BigDecimal("0.00070000"),
        )
        val fee3 = BitcoinFee(
            minimalPerKb = BigDecimal("0.00090000"),
            normalPerKb = BigDecimal("0.00100000"),
            priorityPerKb = BigDecimal("0.00110000"),
        )
        val service = createService(fee1, fee2, fee3)

        val result = service.getFee() as Result.Success

        // trimmed mean: drop min and max, keep middle value
        assertThat(result.data).isEqualTo(fee2)
    }

    @Test
    fun `getFee with five providers returns trimmed mean without extremes`() = runTest {
        val fees = listOf(
            BitcoinFee(BigDecimal("0.00010000"), BigDecimal("0.00010000"), BigDecimal("0.00010000")),
            BitcoinFee(BigDecimal("0.00020000"), BigDecimal("0.00020000"), BigDecimal("0.00020000")),
            BitcoinFee(BigDecimal("0.00030000"), BigDecimal("0.00030000"), BigDecimal("0.00030000")),
            BitcoinFee(BigDecimal("0.00040000"), BigDecimal("0.00040000"), BigDecimal("0.00040000")),
            BitcoinFee(BigDecimal("0.00050000"), BigDecimal("0.00050000"), BigDecimal("0.00050000")),
        )
        val service = createService(*fees.toTypedArray())

        val result = service.getFee() as Result.Success

        // trimmed: drop 0.0001 and 0.0005, mean of 0.0002, 0.0003, 0.0004
        assertThat(result.data).isEqualTo(
            BitcoinFee(
                minimalPerKb = BigDecimal("0.00030000"),
                normalPerKb = BigDecimal("0.00030000"),
                priorityPerKb = BigDecimal("0.00030000"),
            ),
        )
    }

    @Test
    fun `getFee when all providers fail returns first failure`() = runTest {
        val error = BlockchainSdkError.CustomError("fail")
        val provider1 = stubProvider(Result.Failure(error))
        val provider2 = stubProvider(Result.Failure(error))
        val service = BitcoinNetworkService(listOf(provider1, provider2), Blockchain.Bitcoin)

        val result = service.getFee()

        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }

    @Test
    fun `getFee ignores failed providers and aggregates successful ones`() = runTest {
        val fee1 = BitcoinFee(
            minimalPerKb = BigDecimal("0.00010000"),
            normalPerKb = BigDecimal("0.00020000"),
            priorityPerKb = BigDecimal("0.00030000"),
        )
        val fee2 = BitcoinFee(
            minimalPerKb = BigDecimal("0.00030000"),
            normalPerKb = BigDecimal("0.00040000"),
            priorityPerKb = BigDecimal("0.00050000"),
        )
        val provider1 = stubProvider(Result.Success(fee1))
        val failProvider = stubProvider(Result.Failure(BlockchainSdkError.CustomError("fail")))
        val provider2 = stubProvider(Result.Success(fee2))
        val service = BitcoinNetworkService(listOf(provider1, failProvider, provider2), Blockchain.Bitcoin)

        val result = service.getFee() as Result.Success

        // 2 successful providers → arithmetic mean
        assertThat(result.data).isEqualTo(
            BitcoinFee(
                minimalPerKb = BigDecimal("0.00020000"),
                normalPerKb = BigDecimal("0.00030000"),
                priorityPerKb = BigDecimal("0.00040000"),
            ),
        )
    }

    private fun createService(vararg fees: BitcoinFee): BitcoinNetworkService {
        val providers = fees.map { stubProvider(Result.Success(it)) }
        return BitcoinNetworkService(providers, Blockchain.Bitcoin)
    }

    private fun stubProvider(feeResult: Result<BitcoinFee>) = object : BitcoinNetworkProvider {
        override val baseUrl: String = "https://stub"

        override suspend fun getInfo(address: String): Result<BitcoinAddressInfo> = error("Not expected")

        override suspend fun getFee(): Result<BitcoinFee> = feeResult

        override suspend fun sendTransaction(transaction: String): SimpleResult = error("Not expected")

        override suspend fun getSignatureCount(address: String): Result<Int> = error("Not expected")

        override suspend fun getInfoByXpub(xpub: String): Result<XpubInfoResponse> = error("Not expected")

        override suspend fun getUtxoByXpub(xpub: String): Result<List<BitcoinUnspentOutput>> = error("Not expected")
    }
}