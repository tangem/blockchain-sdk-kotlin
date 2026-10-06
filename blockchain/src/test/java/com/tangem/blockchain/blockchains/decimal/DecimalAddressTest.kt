package com.tangem.blockchain.blockchains.decimal

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import com.tangem.common.extensions.hexToBytes
import org.bitcoinj.core.Bech32
import org.junit.Assert.assertThrows
import org.junit.Test

internal class DecimalAddressTest {

    private val addressService = DecimalAddressService()

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = (
            "04BAEC8CD3BA50FDFE1E8CF2B04B58E17041245341CD1F1C6B3A496B48956DB4C896A6848BCF8FCFC33B88341507DD25E5F4609" +
                "386C68086C74CF472B86E5C3820"
            ).hexToBytes()
        val expectedAddress = "0xc63763572D45171e4C25cA0818b44E5Dd7F5c15B"

        Truth.assertThat(addressService.makeAddress(walletPublicKey)).isEqualTo(expectedAddress)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "0xc63763572d45171e4c25ca0818b44e5dd7f5c15b"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun validateCorrectAddressWithChecksum() {
        val ercAddress = "0xc63763572D45171e4C25cA0818b44E5Dd7F5c15B"
        val dscAddress = "d01ccmkx4edg5t3unp9egyp3dzwthtlts2m320gh9"

        Truth.assertThat(addressService.validate(ercAddress)).isTrue()
        Truth.assertThat(addressService.validate(dscAddress)).isTrue()
    }

    @Test
    fun convertDelAddressToDscAddress() {
        val delAddress = "d01ccmkx4edg5t3unp9egyp3dzwthtlts2m320gh9"

        Truth.assertThat(DecimalAddressService.convertDelAddressToDscAddress(delAddress))
            .isEqualTo("0xc63763572d45171e4c25ca0818b44e5dd7f5c15b")
    }

    @Test
    fun validateLegacyAddress() {
        val legacyAddress = "dx1m6kmamcqzy3rx3z4vemc3xd2h0xdmmhlctgy2e"

        Truth.assertThat(addressService.validate(legacyAddress)).isTrue()
        Truth.assertThat(DecimalAddressService.convertDelAddressToDscAddress(legacyAddress))
            .isEqualTo("0xdeadbeef00112233445566778899aabbccddeeff")
    }

    @Test
    fun validateAddressWithAnotherAddressAsPrefix() {
        // bech32 with the payee's full DEL address as the prefix and another wallet in the data part
        val address = "d01ccmkx4edg5t3unp9egyp3dzwthtlts2m320gh91m6kmamcqzy3rx3z4vemc3xd2h0xdmmhl723ar4"

        Truth.assertThat(addressService.validate(address)).isFalse()
        assertThrows(IllegalArgumentException::class.java) {
            DecimalAddressService.convertDelAddressToDscAddress(address)
        }
    }

    @Test
    fun validateBech32mAddress() {
        val address = "d01ccmkx4edg5t3unp9egyp3dzwthtlts2m320gh91m6kmamcqzy3rx3z4vemc3xd2h0xdmmhltkp3xh"

        Truth.assertThat(addressService.validate(address)).isFalse()
    }

    @Test
    fun validateAddressWithUnknownPrefix() {
        val address = Bech32.encode("d0x", "c63763572d45171e4c25ca0818b44e5dd7f5c15b".hexToBytes().toFiveBitGroups())

        Truth.assertThat(addressService.validate(address)).isFalse()
    }

    @Test
    fun validateMalformedAddressReturnsFalse() {
        Truth.assertThat(addressService.validate("d0notabech32address")).isFalse()
    }

    private fun ByteArray.toFiveBitGroups(): ByteArray = Crypto.convertBits(this, 0, size, 8, 5, true)
}