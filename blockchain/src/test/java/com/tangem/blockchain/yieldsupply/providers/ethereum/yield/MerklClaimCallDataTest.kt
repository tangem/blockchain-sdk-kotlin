package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import org.junit.Test

internal class MerklClaimCallDataTest {

    @Test
    fun `WHEN prefixed payload decoded THEN it carries the bytes and its selector`() {
        val actual = MerklClaimCallData.fromHex("0xb4701e3edeadbeef")

        Truth.assertThat(actual).isEqualTo(MerklClaimCallData(byteArrayOf(-76, 112, 30, 62, -34, -83, -66, -17)))
        Truth.assertThat(actual.methodId).isEqualTo("0xb4701e3e")
    }

    @Test
    fun `WHEN unprefixed payload decoded THEN it is the same call data`() {
        val prefixed = MerklClaimCallData.fromHex("0xb4701e3e")
        val bare = MerklClaimCallData.fromHex("b4701e3e")

        Truth.assertThat(bare).isEqualTo(prefixed)
        Truth.assertThat(bare.hashCode()).isEqualTo(prefixed.hashCode())
    }

    @Test
    fun `WHEN payloads differ THEN they are not equal`() {
        val actual = MerklClaimCallData.fromHex("0xb4701e3e")

        Truth.assertThat(actual).isNotEqualTo(MerklClaimCallData.fromHex("0xbebc896c"))
    }

    @Test
    fun `WHEN validated THEN only evm blockchains are accepted`() {
        val callData = MerklClaimCallData.fromHex("0xb4701e3e")

        Truth.assertThat(callData.validate(Blockchain.Ethereum)).isTrue()
        Truth.assertThat(callData.validate(Blockchain.Bitcoin)).isFalse()
    }
}