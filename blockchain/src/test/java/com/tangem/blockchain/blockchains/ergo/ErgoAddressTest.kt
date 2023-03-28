package com.tangem.blockchain.blockchains.ergo

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class ErgoAddressTest {

    private val addressServiceMainnet = ErgoAddressService(false)
    private val addressServiceTestnet = ErgoAddressService(true)

    @Test
    fun makeAddressFromCorrectPublicKey_Mainnet() {
        val walletPublicKey = "0485D520C8B907F0BC5E03FCBBAC212CCD270764BBFF4990A28653A2FB0D656C342DF143C4D52C43582289E20A81D5D014C1384A1FFFEA1D121903AD7ED35A01EA".hexToBytes()
        val expected = "9fLqXN4UnoQhA8oorMVGnLuXzVGDLLtb7pbfPyWMqkLMtCfNQcp"

        Truth.assertThat(addressServiceMainnet.makeAddress(walletPublicKey)).isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress_Mainnet() {
        val address = "9fLqXN4UnoQhA8oorMVGnLuXzVGDLLtb7pbfPyWMqkLMtCfNQcp"

        Truth.assertThat(addressServiceMainnet.validate(address)).isTrue()
    }

    @Test
    fun makeAddressFromCorrectPublicKey_Testnet() {
        val walletPublicKey = "0485D520C8B907F0BC5E03FCBBAC212CCD270764BBFF4990A28653A2FB0D656C342DF143C4D52C43582289E20A81D5D014C1384A1FFFEA1D121903AD7ED35A01EA".hexToBytes()
        val expected = "3WwRz78THFTcHSM6f4fn1ZrhWwhha2gXCBngcfX2DVoisBMEp2rj"

        Truth.assertThat(addressServiceTestnet.makeAddress(walletPublicKey)).isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress_Testnet() {
        val address = "3WwRz78THFTcHSM6f4fn1ZrhWwhha2gXCBngcfX2DVoisBMEp2rj"

        Truth.assertThat(addressServiceTestnet.validate(address)).isTrue()
    }
}