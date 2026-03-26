package com.tangem.blockchain.blockchains.stellar

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.memo.MemoState
import com.tangem.blockchain.extensions.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StellarMemoValidatorTest {

    private val networkProvider = mockk<StellarNetworkProvider>()
    private val validator = StellarMemoValidator(networkProvider)

    @Test
    fun `GIVEN empty memo WHEN validateMemo THEN returns Valid`() = runTest {
        val result = validator.validateMemo("")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Valid)
    }

    @Test
    fun `GIVEN valid uint64 digits WHEN validateMemo THEN returns Valid`() = runTest {
        val result = validator.validateMemo("12345678")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Valid)
    }

    @Test
    fun `GIVEN zero digit WHEN validateMemo THEN returns Valid`() = runTest {
        val result = validator.validateMemo("0")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Valid)
    }

    @Test
    fun `GIVEN max uint64 WHEN validateMemo THEN returns Valid`() = runTest {
        val result = validator.validateMemo("18446744073709551615")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Valid)
    }

    @Test
    fun `GIVEN digits exceeding uint64 WHEN validateMemo THEN returns Invalid`() = runTest {
        val result = validator.validateMemo("18446744073709551616")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Invalid)
    }

    @Test
    fun `GIVEN text within 28 bytes WHEN validateMemo THEN returns Valid`() = runTest {
        val result = validator.validateMemo("short text memo")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Valid)
    }

    @Test
    fun `GIVEN text exactly 28 bytes WHEN validateMemo THEN returns Valid`() = runTest {
        val memo = "a".repeat(28)
        val result = validator.validateMemo(memo)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Valid)
    }

    @Test
    fun `GIVEN text exceeding 28 bytes WHEN validateMemo THEN returns Invalid`() = runTest {
        val memo = "a".repeat(29)
        val result = validator.validateMemo(memo)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Invalid)
    }

    @Test
    fun `GIVEN multibyte utf8 text exceeding 28 bytes WHEN validateMemo THEN returns Invalid`() = runTest {
        // Each Cyrillic char is 2 bytes in UTF-8, so 15 chars = 30 bytes > 28
        val memo = "м".repeat(15)
        val result = validator.validateMemo(memo)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Invalid)
    }

    @Test
    fun `GIVEN memo required by account WHEN isMemoRequired THEN returns true`() = runTest {
        coEvery { networkProvider.checkTargetAccount("GADDR1", null) } returns Result.Success(
            StellarTargetAccountResponse(accountCreated = true, requiresMemo = true),
        )

        val result = validator.isMemoRequired("GADDR1")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isTrue()
    }

    @Test
    fun `GIVEN memo not required by account WHEN isMemoRequired THEN returns false`() = runTest {
        coEvery { networkProvider.checkTargetAccount("GADDR2", null) } returns Result.Success(
            StellarTargetAccountResponse(accountCreated = true, requiresMemo = false),
        )

        val result = validator.isMemoRequired("GADDR2")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isFalse()
    }

    @Test
    fun `GIVEN network error WHEN isMemoRequired THEN returns Failure`() = runTest {
        coEvery { networkProvider.checkTargetAccount("GADDR3", null) } returns Result.Failure(
            com.tangem.blockchain.common.BlockchainSdkError.CustomError("Network error"),
        )

        val result = validator.isMemoRequired("GADDR3")

        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }
}