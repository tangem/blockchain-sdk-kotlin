package com.tangem.blockchain.blockchains.xrp

import com.google.common.truth.Truth
import com.tangem.blockchain.makeAddressWithDefaultType
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class XrpAddressTest {

    private val addressService = XrpAddressService()

    @Test
    fun makeAddressFromCorrectSecpPublicKey() {
        val walletPublicKey = "04D2B9FB288540D54E5B32ECAF0381CD571F97F6F1ECD036B66BB11AA52FFE9981110D883080E2E255C6B1640586F7765E6FAA325D1340F49B56B83D9DE56BC7ED".hexToBytes()
        val expected = "rNxCXgKaCMAmowENKnYa5r8Ue78rjgrM6B"

        Truth.assertThat(addressService.makeAddressWithDefaultType(walletPublicKey))
                .isEqualTo(expected)
    }

    @Test
    fun makeAddressFromCorrectEdPublicKey() {
        val walletPublicKey = "12CC4DE73BACF875D7423D152E46C1A665F1718CBE7CA0FEB2BA28C149E11909"
                .hexToBytes()
        val expected = "rwWMNBs2GtJwfX7YNVV1sUYaPy6DRmDHB4"

        Truth.assertThat(addressService.makeAddressWithDefaultType(walletPublicKey)).isEqualTo(expected)
    }

    @Test
    fun validateCorrectRAddress() {
        val address = "rwWMNBs2GtJwfX7YNVV1sUYaPy6DRmDHB4"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun validateCorrectXAddress() {
        val address = "X75YF1evckmkN9gWKkpN8MhDPeZGyou5x5c6DJT5qAdz1jJ"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun decodeCorrectXAddress() {
        val address = "X75YF1evckmkN9gWKkpN8MhDPeZGyou5x5c6DJT5qAdz1jJ"
        val expectedAddress = "rnbz2HXVqtsFouFEwbDuhL7doVxirBz2r6"
        val expectedTag = 555L

        val decoded = XrpAddressService.decodeXAddress(address)

        Truth.assertThat(decoded!!.address).isEqualTo(expectedAddress)
        Truth.assertThat(decoded.destinationTag).isEqualTo(expectedTag)
    }
}
