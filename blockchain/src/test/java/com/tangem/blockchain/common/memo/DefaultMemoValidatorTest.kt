package com.tangem.blockchain.common.memo

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.extensions.Result
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultMemoValidatorTest {

    @Test
    fun `GIVEN any address WHEN isMemoRequired THEN returns false`() = runTest {
        val result = DefaultMemoValidator.isMemoRequired("anyAddress")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isFalse()
    }

    @Test
    fun `GIVEN any memo WHEN validateMemo THEN returns NotSupported`() = runTest {
        val result = DefaultMemoValidator.validateMemo("some memo")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.NotSupported)
    }

    @Test
    fun `GIVEN empty memo WHEN validateMemo THEN returns NotSupported`() = runTest {
        val result = DefaultMemoValidator.validateMemo("")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(MemoState.NotSupported)
    }
}