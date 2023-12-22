package com.tangem.blockchain.common

import com.google.common.truth.Truth
import com.tangem.blockchain.common.derivation.DerivationStyle
import org.junit.Test

internal class DerivationStyleTests {

    @Test
    fun testDerivationStyles() {
        val legacy = DerivationStyle.LEGACY
        val new = DerivationStyle.NEW

        val fantom = Blockchain.Fantom
        Truth.assertThat(fantom.derivationPath(legacy)!!.rawPath)
            .isEqualTo("m/44'/1007'/0'/0/0")
        Truth.assertThat(fantom.derivationPath(new)!!.rawPath)
            .isEqualTo("m/44'/60'/0'/0/0")

        val eth = Blockchain.Ethereum
        Truth.assertThat(eth.derivationPath(legacy)!!.rawPath)
            .isEqualTo("m/44'/60'/0'/0/0")
        Truth.assertThat(eth.derivationPath(new)!!.rawPath)
            .isEqualTo("m/44'/60'/0'/0/0")

        val ethTestnet = Blockchain.EthereumTestnet
        Truth.assertThat(ethTestnet.derivationPath(legacy)!!.rawPath)
            .isEqualTo("m/44'/1'/0'/0/0")
        Truth.assertThat(ethTestnet.derivationPath(new)!!.rawPath)
            .isEqualTo("m/44'/1'/0'/0/0")

        val xrp = Blockchain.XRP
        Truth.assertThat(xrp.derivationPath(legacy)!!.rawPath)
            .isEqualTo("m/44'/144'/0'/0/0")
        Truth.assertThat(xrp.derivationPath(new)!!.rawPath)
            .isEqualTo("m/44'/144'/0'/0/0")
    }
}
