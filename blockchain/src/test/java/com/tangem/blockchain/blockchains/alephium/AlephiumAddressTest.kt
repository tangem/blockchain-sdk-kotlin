package com.tangem.blockchain.blockchains.alephium

import com.tangem.common.extensions.hexToBytes
import junit.framework.TestCase.assertEquals
import org.junit.Test

class AlephiumAddressTest {

    private val walletPublicKey = (
        "04EB30400CE9D1DEED12B84D4161A1FA922EF4185A155EF3EC208078B3807B126FA22C335081AAEBF161095C11C7D8BD550EF88" +
            "82A3125B0EE9AE96DDDE1AE743F"
        ).hexToBytes()
    private val alephiumAddressService = AlephiumAddressService()

    @Test
    fun checkCorrectAddress() {
        val address = alephiumAddressService.makeAddress(walletPublicKey)
        val expected = "151nWtRBoUVrLWf4ipX4uGjsTgNHob6aBeuWbE2eMBtm6"
        assertEquals(address, expected)
    }

    @Test
    fun checkValidateAddress() {
        val isValid = alephiumAddressService.validate("151nWtRBoUVrLWf4ipX4uGjsTgNHob6aBeuWbE2eMBtm6")
        assert(isValid)
    }
}