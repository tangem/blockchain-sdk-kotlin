package com.tangem.blockchain.blockchains.cardano

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.wrapInObject
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class CardanoAddressTest {

    private val addressService = CardanoAddressService(Blockchain.Cardano)

    @Test
    fun makeAddressesFromCorrectPublicKey() {
        val walletPublicKey = "EC5387D8B38BD9EF80BDBC78D0D7E1C53F08E269436C99D5B3C2DF4B2CE73012"
            .hexToBytes()
        val expectedByronAddress = "Ae2tdPwUPEZB972NhMM1dqixaUjnveaic6A23bprgrhvvgbkx2zaezrLY2Y"
        val expectedShelleyAddress = "addr1vyfgrxddyvyaqhr4jprr655s8ehzna9nehanx3fmu9280cgxxg2zc"

        val byronAddress =
            addressService.makeAddress(walletPublicKey.wrapInObject(), AddressType.Legacy, EllipticCurve.Secp256k1)
        val shelleyAddress =
            addressService.makeAddress(walletPublicKey.wrapInObject(), AddressType.Default, EllipticCurve.Secp256k1)

        Truth.assertThat(byronAddress.value).isEqualTo(expectedByronAddress)
        Truth.assertThat(shelleyAddress.value).isEqualTo(expectedShelleyAddress)
    }

    @Test
    fun validateCorrectByronAddress() {
        val address = "Ae2tdPwUPEZB972NhMM1dqixaUjnveaic6A23bprgrhvvgbkx2zaezrLY2Y"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }

    @Test
    fun validateCorrectShelleyAddress() {
        val address = "addr1vyfgrxddyvyaqhr4jprr655s8ehzna9nehanx3fmu9280cgxxg2zc"

        Truth.assertThat(addressService.validate(address)).isTrue()
    }
}