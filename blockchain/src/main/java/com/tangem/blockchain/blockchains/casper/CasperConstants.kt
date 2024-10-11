package com.tangem.blockchain.blockchains.casper

import com.tangem.common.card.EllipticCurve

internal object CasperConstants {
    const val ED25519_PREFIX = "01"
    const val ED25519_LENGTH = 66

    const val SECP256K1_PREFIX = "02"
    const val SECP256K1_LENGTH = 68

    fun getAddressPrefix(curve: EllipticCurve) = when (curve) {
        EllipticCurve.Ed25519,
        EllipticCurve.Ed25519Slip0010,
        -> ED25519_PREFIX
        EllipticCurve.Secp256k1,
        -> SECP256K1_PREFIX
        // added as unsupported for now, need to research
        EllipticCurve.Secp256r1,
        EllipticCurve.Bls12381G2,
        EllipticCurve.Bls12381G2Aug,
        EllipticCurve.Bls12381G2Pop,
        EllipticCurve.Bip0340,
        -> error("${curve.curve} is not supported")
    }
}