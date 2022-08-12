package com.tangem.blockchain_demo.extensions

import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.card.Card

/**
 * Created by Anton Zhilenkov on 12/08/2022.
 */
val Card.useOldStyleDerivation: Boolean
    get() = batchId == "AC01" || batchId == "AC02" || batchId == "CB95"

val Card.derivationStyle: DerivationStyle?
    get() = if (!settings.isHDWalletAllowed) {
        null
    } else if (useOldStyleDerivation) {
        DerivationStyle.LEGACY
    } else {
        DerivationStyle.NEW
    }