package com.tangem.demo.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve

/**
[REDACTED_AUTHOR]
 */
fun Blockchain.Companion.valuesWithoutUnknown(): List<Blockchain> {
    return Blockchain.values().toMutableList().apply { remove(Blockchain.Unknown) }.toList()
}

fun Blockchain.getPrimaryCurve(): EllipticCurve? {
    return when {
        getSupportedCurves().contains(EllipticCurve.Secp256k1) -> {
            EllipticCurve.Secp256k1
        }
        getSupportedCurves().contains(EllipticCurve.Ed25519) -> {
            EllipticCurve.Ed25519
        }
        else -> {
            null
        }
    }
}