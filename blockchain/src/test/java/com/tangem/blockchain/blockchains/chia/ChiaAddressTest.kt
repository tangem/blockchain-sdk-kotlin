package com.tangem.blockchain.blockchains.chia

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.chia.clvm.CreateCoinCondition
import com.tangem.blockchain.blockchains.chia.clvm.Program
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toByteArray
import com.tangem.common.extensions.toHexString
import org.junit.Test
import java.math.BigInteger

class ChiaAddressTest {

    private val chiaAddressService = ChiaAddressService(Blockchain.Chia)
    private val chiaTestnetAddressService = ChiaAddressService(Blockchain.ChiaTestnet)

    @Test
    fun makeAddressFromCorrectPublicKey() {
        val walletPublicKey = "b8f7dd239557ff8c49d338f89ac1a258a863fa52cd0a502e3aaae4b6738ba39ac8d982215aa3fa16bc5f8cb7e44b954d".hexToBytes()
        val expected = "xch14gxuvfmw2xdxqnws5agt3ma483wktd2lrzwvpj3f6jvdgkmf5gtq20kt0z"
        val expectedTestnet = "txch14gxuvfmw2xdxqnws5agt3ma483wktd2lrzwvpj3f6jvdgkmf5gtq8g3aw3"

        Truth.assertThat(chiaAddressService.makeAddress(walletPublicKey)).isEqualTo(expected)
        Truth.assertThat(chiaTestnetAddressService.makeAddress(walletPublicKey)).isEqualTo(expectedTestnet)
    }

    @Test
    fun validateCorrectAddress() {
        val address = "xch14gxuvfmw2xdxqnws5agt3ma483wktd2lrzwvpj3f6jvdgkmf5gtq20kt0z"
        val addressTestnet = "txch14gxuvfmw2xdxqnws5agt3ma483wktd2lrzwvpj3f6jvdgkmf5gtq8g3aw3"

        Truth.assertThat(chiaAddressService.validate(address)).isTrue()
        Truth.assertThat(chiaTestnetAddressService.validate(addressTestnet)).isTrue()
    }
}