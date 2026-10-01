package com.tangem.blockchain.blockchains.bitcoincash

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.hexToBytes
import org.junit.Assert.assertThrows
import org.junit.Test

class BitcoinCashAddressTest {

    private val addressService = BitcoinCashAddressService(Blockchain.BitcoinCash)

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = (
            "04BE37CD5251C8999EDBBFC759D800EB41E4DCB718289601EB15819404E1B2F2ED90FE50C2A481D06EC790D1EF6184974EB655A" +
                "BAE4BE56A6D1C9E1A17B1EFDF02"
            ).hexToBytes()
        val expected = "bitcoincash:qp7atyzvetwq8a0x02y2snvnns5jfwnzacf9vfa4x3"

        Truth.assertThat(addressService.makeAddress(walletPublicKey))
            .isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "bitcoincash:qp7atyzvetwq8a0x02y2snvnns5jfwnzacf9vfa4x3"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun validateCorrectScriptHashAddress() {
        val address = "bitcoincash:ppm2qsznhks23z7629mms6s4cwef74vcwvn0h829pq"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun validateIncorrectAddress() {
        val address = "bitcoincash:cpm2qsznhks23z7629mms6s4cwef74vcwvn0h829pq"

        Truth.assertThat(addressService.validate(address)).isFalse()
    }

    @Test
    fun validateAddressWithMultipleSeparators() {
        // checksum of the first payload is valid, the second payload belongs to another wallet
        val address = "bitcoincash:qzdq69mpdlgh8wxg8m7zmt2qdd5j6q9w7yfmcl8tnm:" +
            "qq8tgddc8zpp45c7hafckg4v702v0ka6eqfmcl8tnm"

        Truth.assertThat(addressService.validate(address)).isFalse()
        Truth.assertThat(addressService.validateCashAddrAddress(address)).isFalse()
    }

    @Test
    fun validateAddressWithTrailingSeparator() {
        val address = "bitcoincash:qp7atyzvetwq8a0x02y2snvnns5jfwnzacf9vfa4x3:"

        Truth.assertThat(addressService.validate(address)).isFalse()
    }

    @Test
    fun getPublicKeyHashFromAddressWithMultipleSeparatorsThrows() {
        val address = "bitcoincash:qzdq69mpdlgh8wxg8m7zmt2qdd5j6q9w7yfmcl8tnm:" +
            "qq8tgddc8zpp45c7hafckg4v702v0ka6eqfmcl8tnm"

        assertThrows(RuntimeException::class.java) { addressService.getPublicKeyHash(address) }
    }
}