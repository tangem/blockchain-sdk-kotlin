package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import java.math.BigInteger

internal class EthereumYieldSupplySwapCallDataTest {
    private val blockchain = Blockchain.Ethereum

    private val tokenIn = "0x1234567890abcdef1234567890abcdef12345678"
    private val amountIn = BigInteger("100000000000000000000")
    private val target = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
    private val spender = "0x1111111111111111111111111111111111111111"
    private val swapData = "0xdeadbeef".hexToBytes()

    @Test
    fun `Is call data correct`() {
        val callData = EthereumYieldSupplySwapCallData(
            tokenIn = tokenIn,
            amountIn = amountIn,
            target = target,
            spender = spender,
            swapData = swapData,
        )
        val expected = "0x4c3f521d".hexToBytes() +
            "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes() +
            "0000000000000000000000000000000000000000000000056bc75e2d63100000".hexToBytes() +
            "000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd".hexToBytes() +
            "0000000000000000000000001111111111111111111111111111111111111111".hexToBytes() +
            "00000000000000000000000000000000000000000000000000000000000000a0".hexToBytes() +
            "0000000000000000000000000000000000000000000000000000000000000004".hexToBytes() +
            "deadbeef00000000000000000000000000000000000000000000000000000000".hexToBytes()
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `Is call data correct with empty swap data`() {
        val callData = EthereumYieldSupplySwapCallData(
            tokenIn = tokenIn,
            amountIn = amountIn,
            target = target,
            spender = spender,
            swapData = byteArrayOf(),
        )
        val expected = "0x4c3f521d".hexToBytes() +
            "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes() +
            "0000000000000000000000000000000000000000000000056bc75e2d63100000".hexToBytes() +
            "000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd".hexToBytes() +
            "0000000000000000000000001111111111111111111111111111111111111111".hexToBytes() +
            "00000000000000000000000000000000000000000000000000000000000000a0".hexToBytes() +
            "0000000000000000000000000000000000000000000000000000000000000000".hexToBytes()
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `Validate call data with valid params`() {
        val callData = EthereumYieldSupplySwapCallData(
            tokenIn = tokenIn,
            amountIn = amountIn,
            target = target,
            spender = spender,
            swapData = swapData,
        )
        Truth.assertThat(callData.validate(blockchain)).isTrue()
    }

    @Test
    fun `Validate call data with zero address`() {
        Truth.assertThat(
            EthereumYieldSupplySwapCallData(
                tokenIn = EthereumUtils.ZERO_ADDRESS,
                amountIn = amountIn,
                target = target,
                spender = spender,
                swapData = swapData,
            ).validate(blockchain),
        ).isFalse()
    }

    @Test
    fun `Validate call data with zero amount`() {
        Truth.assertThat(
            EthereumYieldSupplySwapCallData(
                tokenIn = tokenIn,
                amountIn = BigInteger.ZERO,
                target = target,
                spender = spender,
                swapData = swapData,
            ).validate(blockchain),
        ).isFalse()
    }
}