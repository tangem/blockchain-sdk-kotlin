package com.tangem.blockchain.blockchains.xrp

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.common.memo.MemoState
import com.tangem.blockchain.extensions.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class XrpMemoValidatorTest {

    private val networkProvider = mockk<XrpNetworkProvider>()
    private val validator = XrpMemoValidator(networkProvider)

    @Test
    fun `GIVEN empty memo WHEN validateMemo THEN returns Valid`() = runTest {
        val result = validator.validateMemo("")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Valid)
    }

    @Test
    fun `GIVEN zero tag WHEN validateMemo THEN returns Valid`() = runTest {
        val result = validator.validateMemo("0")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Valid)
    }

    @Test
    fun `GIVEN max uint32 tag WHEN validateMemo THEN returns Valid`() = runTest {
        val result = validator.validateMemo("4294967295")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Valid)
    }

    @Test
    fun `GIVEN tag exceeding uint32 WHEN validateMemo THEN returns Invalid`() = runTest {
        val result = validator.validateMemo("4294967296")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Invalid)
    }

    @Test
    fun `GIVEN negative tag WHEN validateMemo THEN returns Invalid`() = runTest {
        val result = validator.validateMemo("-1")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Invalid)
    }

    @Test
    fun `GIVEN text memo WHEN validateMemo THEN returns Invalid`() = runTest {
        val result = validator.validateMemo("hello")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Invalid)
    }

    @Test
    fun `GIVEN valid mid-range tag WHEN validateMemo THEN returns Valid`() = runTest {
        val result = validator.validateMemo("123456")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.Valid)
    }

    // endregion

    // region isMemoRequired

    @Test
    fun `GIVEN destination tag required WHEN isMemoRequired THEN returns true`() = runTest {
        coEvery { networkProvider.checkDestinationTagRequired("rAddress1") } returns true

        val result = validator.isMemoRequired("rAddress1")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isTrue()
    }

    @Test
    fun `GIVEN destination tag not required WHEN isMemoRequired THEN returns false`() = runTest {
        coEvery { networkProvider.checkDestinationTagRequired("rAddress2") } returns false

        val result = validator.isMemoRequired("rAddress2")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isFalse()
    }

    @Test
    fun `GIVEN network error WHEN isMemoRequired THEN returns Failure`() = runTest {
        coEvery { networkProvider.checkDestinationTagRequired("rAddress3") } throws RuntimeException("Network error")

        val result = validator.isMemoRequired("rAddress3")

        assertThat(result).isInstanceOf(Result.Failure::class.java)
    }
}