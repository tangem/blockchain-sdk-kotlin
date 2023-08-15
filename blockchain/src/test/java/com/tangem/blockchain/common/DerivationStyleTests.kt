package com.tangem.blockchain.common

import com.google.common.truth.Truth
import org.junit.Test

internal class DerivationStyleTests {


    @Test
    fun testDerivationStyles() {
        val legacy = DerivationStyle.LEGACY
        val new = DerivationStyle.NEW

        val fantom = Blockchain.Fantom
        Truth.assertThat(fantom.derivationPathOldStyle(legacy)!!.rawPath)
            .isEqualTo("m/44'/1007'/0'/0/0")
        Truth.assertThat(fantom.derivationPathOldStyle(new)!!.rawPath)
            .isEqualTo("m/44'/60'/0'/0/0")

        val eth = Blockchain.Ethereum
        Truth.assertThat(eth.derivationPathOldStyle(legacy)!!.rawPath)
            .isEqualTo("m/44'/60'/0'/0/0")
        Truth.assertThat(eth.derivationPathOldStyle(new)!!.rawPath)
            .isEqualTo("m/44'/60'/0'/0/0")

        val ethTestnet = Blockchain.EthereumTestnet
        Truth.assertThat(ethTestnet.derivationPathOldStyle(legacy)!!.rawPath)
            .isEqualTo("m/44'/1'/0'/0/0")
        Truth.assertThat(ethTestnet.derivationPathOldStyle(new)!!.rawPath)
            .isEqualTo("m/44'/1'/0'/0/0")

        val xrp = Blockchain.XRP
        Truth.assertThat(xrp.derivationPathOldStyle(legacy)!!.rawPath)
            .isEqualTo("m/44'/144'/0'/0/0")
        Truth.assertThat(xrp.derivationPathOldStyle(new)!!.rawPath)
            .isEqualTo("m/44'/144'/0'/0/0")
    }

}