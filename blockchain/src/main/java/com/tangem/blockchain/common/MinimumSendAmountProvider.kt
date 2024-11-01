package com.tangem.blockchain.common

import java.math.BigDecimal

interface MinimumSendAmountProvider {
    fun getMinimumSendAmount(): BigDecimal
}