package com.tangem.blockchain.yieldsupply.providers.ethereum.registry

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class EthereumYieldSupplyAllowedAddressCallDataTest {
    private val blockchain = Blockchain.Ethereum

    @Test
    fun `Is spender call data correct`() {
        val spenderAddress = "0x1234567890abcdef1234567890abcdef12345678"
        val callData = EthereumYieldSupplyAllowedAddressCallData.allowedSpenders(spenderAddress)
        val expected = "0xd8528af0".hexToBytes() +
            "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes()
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `Is target call data correct`() {
        val targetAddress = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
        val callData = EthereumYieldSupplyAllowedAddressCallData.allowedTargets(targetAddress)
        val expected = "0xb8fe8d5f".hexToBytes() +
            "000000000000000000000000abcdefabcdefabcdefabcdefabcdefabcdefabcd".hexToBytes()
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `Validate spender call data`() {
        val address = "0x1234567890abcdef1234567890abcdef12345678"
        Truth.assertThat(
            EthereumYieldSupplyAllowedAddressCallData.allowedSpenders(address).validate(blockchain),
        ).isTrue()
        Truth.assertThat(
            EthereumYieldSupplyAllowedAddressCallData.allowedSpenders("").validate(blockchain),
        ).isFalse()
        Truth.assertThat(
            EthereumYieldSupplyAllowedAddressCallData.allowedSpenders(EthereumUtils.ZERO_ADDRESS)
                .validate(blockchain),
        ).isFalse()
    }

    @Test
    fun `Validate target call data`() {
        val address = "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"
        Truth.assertThat(
            EthereumYieldSupplyAllowedAddressCallData.allowedTargets(address).validate(blockchain),
        ).isTrue()
        Truth.assertThat(
            EthereumYieldSupplyAllowedAddressCallData.allowedTargets("").validate(blockchain),
        ).isFalse()
        Truth.assertThat(
            EthereumYieldSupplyAllowedAddressCallData.allowedTargets(EthereumUtils.ZERO_ADDRESS)
                .validate(blockchain),
        ).isFalse()
    }
}