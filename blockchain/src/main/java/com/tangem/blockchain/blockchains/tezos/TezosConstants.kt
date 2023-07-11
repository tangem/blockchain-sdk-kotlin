package com.tangem.blockchain.blockchains.tezos

import com.tangem.common.card.EllipticCurve

class TezosConstants {
    companion object {
        const val TZ1_PREFIX = "06A19F"
        const val TZ2_PREFIX = "06A1A1"
        const val TZ3_PREFIX = "06A1A4"
        const val KT1_PREFIX = "025A79"

        const val EDPK_PREFIX = "0D0F25D9"
        const val SPPK_PREFIX = "03FEE256"
        const val P2PK_PREFIX = "03B28B7F"

        const val EDSIG_PREFIX = "09F5CD8612"
        const val SPSIG_PREFIX = "0D7365133F"
        const val P2SIG_PREFIX = "36F02C34"

        const val BRANCH_PREFIX = "0134"
        const val REVEAL_OPERATION_KIND = "6b"
        const val TRANSACTION_OPERATION_KIND = "6c"
        const val GENERIC_OPERATION_WATERMARK = "03"

        const val TRANSACTION_FEE = 0.00142
        const val REVEAL_FEE = 0.0013
        const val ALLOCATION_FEE = 0.06425

        fun getAddressPrefix(curve: EllipticCurve) = when (curve) {
            EllipticCurve.Ed25519 -> TZ1_PREFIX
            EllipticCurve.Secp256k1 -> TZ2_PREFIX
            // added as unsupported for now, need to research
            EllipticCurve.Secp256r1,
            EllipticCurve.Bls12381G2,
            EllipticCurve.Bls12381G2Aug,
            EllipticCurve.Bls12381G2Pop,
            EllipticCurve.Bip0340,
            -> error("${curve.curve} is not supported")
        }

        fun getPublicKeyPrefix(curve: EllipticCurve) = when (curve) {
            EllipticCurve.Ed25519 -> EDPK_PREFIX
            EllipticCurve.Secp256k1 -> SPPK_PREFIX
            // added as unsupported for now, need to research
            EllipticCurve.Secp256r1,
            EllipticCurve.Bls12381G2,
            EllipticCurve.Bls12381G2Aug,
            EllipticCurve.Bls12381G2Pop,
            EllipticCurve.Bip0340,
            -> error("${curve.curve} is not supported")
        }

        fun getSignaturePrefix(curve: EllipticCurve) = when (curve) {
            EllipticCurve.Ed25519 -> EDSIG_PREFIX
            EllipticCurve.Secp256k1 -> SPSIG_PREFIX
            // added as unsupported for now, need to research
            EllipticCurve.Secp256r1,
            EllipticCurve.Bls12381G2,
            EllipticCurve.Bls12381G2Aug,
            EllipticCurve.Bls12381G2Pop,
            EllipticCurve.Bip0340,
            -> error("${curve.curve} is not supported")
        }
    }
}