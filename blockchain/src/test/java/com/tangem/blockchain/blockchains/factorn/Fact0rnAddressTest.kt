package com.tangem.blockchain.blockchains.factorn

import com.google.common.truth.Truth
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.extensions.hexToBytes
import org.bitcoinj.core.SegwitAddress
import org.bitcoinj.script.ScriptBuilder
import org.junit.Test

class Fact0rnAddressTest {

    private val addressService = Fact0rnAddressService()
    private val expectedAddress = "fact1qg2qvzvrgukkp5gct2n8dvuxz99ddxwecmx9sey"

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "03B6D7E1FB0977A5881A3B1F64F9778B4F56CB2B9EFD6E0D03E60051EAFEBF5831".hexToBytes()
        val expectedSize = 1

        val addresses = addressService.makeAddresses(walletPublicKey)
        val address = requireNotNull(addresses.find { it.type == AddressType.Default })

        Truth.assertThat(addresses.size).isEqualTo(expectedSize)
        Truth.assertThat(address.value).isEqualTo(expectedAddress)
    }

    @Test
    fun makeScriptHashFromAddress() {
        val expectedScriptHash = "808171256649754B402099695833B95E4507019B3E494A7DBC6F62058F09050E"
        Truth.assertThat(Fact0rnAddressService.addressToScriptHash(expectedAddress)).isEqualTo(expectedScriptHash)
    }

    @Test
    fun makeScriptFromAddress() {
        val expectedScript = ScriptBuilder
            .createOutputScript(SegwitAddress.fromBech32(Fact0rnMainNetParams(), expectedAddress))
        Truth.assertThat(Fact0rnAddressService.addressToScript(expectedAddress)).isEqualTo(expectedScript)
    }
}