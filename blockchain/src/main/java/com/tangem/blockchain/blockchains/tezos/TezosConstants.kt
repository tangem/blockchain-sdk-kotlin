package com.tangem.blockchain.blockchains.tezos

import com.tangem.common.extensions.hexToBytes

class TezosConstants {
    companion object {
        const val TZ1_PREFIX = "06A19F"
        const val TZ2_PREFIX = "06A1A1"
        const val TZ3_PREFIX = "06A1A4"
        const val KT1_PREFIX = "025A79"

        const val EDPK_PREFIX = "0D0F25D9"
        const val SPPK_PREFIX = "03FEE256"
        const val P2PK_PREFIX = "03B28B7F"

        const val BRANCH_PREFIX = "0134"
        const val REVEAL_OPERATION_KIND = "6b"
        const val TRANSACTION_OPERATION_KIND = "6c"
        const val GENERIC_OPERATION_WATERMARK = "03"

        const val TRANSACTION_FEE = 0.00142
        const val REVEAL_FEE = 0.0013
        const val ALLOCATION_FEE = 0.06425
    }
}