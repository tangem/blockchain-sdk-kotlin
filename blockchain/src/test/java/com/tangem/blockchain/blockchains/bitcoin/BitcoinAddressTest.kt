package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class BitcoinAddressTest {

    private val addressService = BitcoinAddressService(Blockchain.Bitcoin)

    @Test
    fun makeAddressesFromCorrectPublicKey() {
        val walletPublicKey = (
            "04752A727E14BBA5BD73B6714D72500F61FFD11026AD1196D2E1C54577CBEEAC3D11FC68A64700F8D533F4E311964EA8FB3AA26" +
                "C588295F2133868D69C3E628693"
            ).hexToBytes()
        val expectedSize = 2
        val expectedLegacyAddress = "1D3vYSjCvzrsVVK5bNaPTjU3NxcN7NNXMN"
        val expectedSegwitAddress = "bc1qtdsvnsf7cfu8l3w7qahwwhhxfrdzmdhsgdzky2"

        val addresses = addressService.makeAddresses(walletPublicKey)
        val legacyAddress = addresses.find { it.type == AddressType.Legacy }
        val segwitAddress = addresses.find { it.type == AddressType.Default }

        Truth.assertThat(addresses.size).isEqualTo(expectedSize)
        Truth.assertThat(legacyAddress!!.value).isEqualTo(expectedLegacyAddress)
        Truth.assertThat(segwitAddress!!.value).isEqualTo(expectedSegwitAddress)
    }

    @Test
    fun makeMultisigAddressesFromCorrectPublicKeys() {
        val walletPublicKey1 = (
            "04752A727E14BBA5BD73B6714D72500F61FFD11026AD1196D2E1C54577CBEEAC3D11FC68A64700F8D533F4E311964EA8FB3AA26" +
                "C588295F2133868D69C3E628693"
            ).hexToBytes()
        val walletPublicKey2 = (
            "04E3F3BE3CE3D8284DB3BA073AD0291040093D83C11A277B905D5555C9EC41073E103F4D9D299EDEA8285C51C3356A8681A5456" +
                "18C174251B984DF841F49D2376F"
            ).hexToBytes()
        val expectedSize = 2
        val expectedLegacyAddress = "358vzrRZUDZ8DM5Zbz9oLqGr8voPYQqe56"
        val expectedSegwitAddress = "bc1qw9czf0m0eu0v5uhdqj9l4w9su3ca0pegzxxk947hrehma343qwusy4nf8c"

        val addresses = addressService.makeMultisigAddresses(walletPublicKey1, walletPublicKey2)
        val addressesReverseOrder =
            addressService.makeMultisigAddresses(walletPublicKey2, walletPublicKey1)
        val legacyAddress = addresses.find { it.type == AddressType.Legacy }
        val segwitAddress = addresses.find { it.type == AddressType.Default }

        Truth.assertThat(addresses.size).isEqualTo(expectedSize)
        Truth.assertThat(addresses.map { it.value })
            .isEqualTo(addressesReverseOrder.map { it.value })
        Truth.assertThat(legacyAddress!!.value).isEqualTo(expectedLegacyAddress)
        Truth.assertThat(segwitAddress!!.value).isEqualTo(expectedSegwitAddress)
    }

    @Test
    fun validateCorrectLegacyAddress() {
        val address = "1D3vYSjCvzrsVVK5bNaPTjU3NxcN7NNXMN"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun validateCorrectSegwitAddress() {
        val address = "bc1qtdsvnsf7cfu8l3w7qahwwhhxfrdzmdhsgdzky2"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }
}
