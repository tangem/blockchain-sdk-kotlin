package com.tangem.blockchain.blockchains.stellar

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class StellarAddressTest {

    private val addressService = StellarAddressService()

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "EC5387D8B38BD9EF80BDBC78D0D7E1C53F08E269436C99D5B3C2DF4B2CE73012"
            .hexToBytes()
        val expected = "GDWFHB6YWOF5T34AXW6HRUGX4HCT6CHCNFBWZGOVWPBN6SZM44YBFUDZ"

        Truth.assertThat(addressService.makeAddress(walletPublicKey)).isEqualTo(expected)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "GDWFHB6YWOF5T34AXW6HRUGX4HCT6CHCNFBWZGOVWPBN6SZM44YBFUDZ"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun validateCorrectContractAddress() {
        val address = "VELO-GDM4RQUQQUVSKQA7S6EM7XBZP3FCGH4Q7CL6TABQ7B2BEJ5ERARM2M5M"
        Truth.assertThat(addressService.validateContractAddress(address)).isTrue()
    }

    @Test
    fun validateIncorrectContractAddress() {
        val address = "GDM4RQUQQUVSKQA7S6EM7XBZP3FCGH4Q7CL6TABQ7B2BEJ5ERARM2M5M"
        Truth.assertThat(addressService.validateContractAddress(address)).isFalse()
    }

    @Test
    fun validateCorrectContractAddressRemoveSuffix() {
        val addressWithSuffix = "VELO-GDM4RQUQQUVSKQA7S6EM7XBZP3FCGH4Q7CL6TABQ7B2BEJ5ERARM2M5M-1"
        Truth.assertThat(addressService.validateContractAddress(addressWithSuffix)).isTrue()
    }
}