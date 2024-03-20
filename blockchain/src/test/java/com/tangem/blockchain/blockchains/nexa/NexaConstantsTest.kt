package com.tangem.blockchain.blockchains.nexa

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchain.common.derivation.DerivationConfigV1
import com.tangem.blockchain.common.derivation.DerivationConfigV2
import com.tangem.blockchain.common.derivation.DerivationConfigV3
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath
import org.junit.Test

class NexaConstantsTest {

    private val decimals = 2
    private val derivations = mapOf(AddressType.Default to DerivationPath("m/44'/29223'/0'/0/0"))
    private val supportedCurves = listOf(EllipticCurve.Secp256k1)

    @Test
    fun correctDerivationPathConfigV1() {
        Truth.assertThat(DerivationConfigV1.derivations(Blockchain.Nexa))
            .isEqualTo(derivations)
    }

    @Test
    fun correctDerivationPathConfigV1Testnet() {
        Truth.assertThat(DerivationConfigV1.derivations(Blockchain.NexaTestnet))
            .isEqualTo(derivations)
    }

    @Test
    fun correctDerivationPathConfigV2() {
        Truth.assertThat(DerivationConfigV2.derivations(Blockchain.Nexa))
            .isEqualTo(derivations)
    }

    @Test
    fun correctDerivationPathConfigV2Testnet() {
        Truth.assertThat(DerivationConfigV2.derivations(Blockchain.NexaTestnet))
            .isEqualTo(derivations)
    }

    @Test
    fun correctDerivationPathConfigV3() {
        Truth.assertThat(DerivationConfigV3.derivations(Blockchain.Nexa))
            .isEqualTo(derivations)
    }

    @Test
    fun correctDerivationPathConfigV3Testnet() {
        Truth.assertThat(DerivationConfigV3.derivations(Blockchain.NexaTestnet))
            .isEqualTo(derivations)
    }

    @Test
    fun correctDecimals() {
        Truth.assertThat(Blockchain.Nexa.decimals()).isEqualTo(decimals)
    }

    @Test
    fun correctDecimalsTestnet() {
        Truth.assertThat(Blockchain.NexaTestnet.decimals()).isEqualTo(decimals)
    }

    @Test
    fun correctCurve() {
        Truth.assertThat(Blockchain.Nexa.getSupportedCurves())
            .isEqualTo(supportedCurves)
    }

    @Test
    fun correctCurveTestnet() {
        Truth.assertThat(Blockchain.NexaTestnet.getSupportedCurves())
            .isEqualTo(supportedCurves)
    }
}