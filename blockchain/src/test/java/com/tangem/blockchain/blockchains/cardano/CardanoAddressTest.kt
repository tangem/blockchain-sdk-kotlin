package com.tangem.blockchain.blockchains.cardano


import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class CardanoAddressTest {

    private val addressService = CardanoAddressService(Blockchain.CardanoShelley)

    @Test
    fun makeAddressesFromCorrectPublicKey() {
        val walletPublicKey = "EC5387D8B38BD9EF80BDBC78D0D7E1C53F08E269436C99D5B3C2DF4B2CE73012"
                .hexToBytes()
        val expectedSize = 2
        val expectedByronAddress = "Ae2tdPwUPEZB972NhMM1dqixaUjnveaic6A23bprgrhvvgbkx2zaezrLY2Y"
        val expectedShelleyAddress = "addr1vyfgrxddyvyaqhr4jprr655s8ehzna9nehanx3fmu9280cgxxg2zc"

        val addresses = addressService.makeAddresses(walletPublicKey)
        val byronAddress = addresses.find { it.type == AddressType.Legacy }
        val shelleyAddress = addresses.find { it.type == AddressType.Default }

        Truth.assertThat(addresses.size).isEqualTo(expectedSize)
        Truth.assertThat(byronAddress!!.value).isEqualTo(expectedByronAddress)
        Truth.assertThat(shelleyAddress!!.value).isEqualTo(expectedShelleyAddress)
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