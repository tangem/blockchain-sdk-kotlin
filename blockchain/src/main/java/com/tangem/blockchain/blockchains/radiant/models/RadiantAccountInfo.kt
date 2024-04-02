package com.tangem.blockchain.blockchains.radiant.models

import com.tangem.blockchain.network.electrum.ElectrumUnspentUTXORecord
import java.math.BigDecimal

internal data class RadiantAccountInfo(
    val balance: BigDecimal,
    val unspentOutputs: List<ElectrumUnspentUTXORecord>,
)