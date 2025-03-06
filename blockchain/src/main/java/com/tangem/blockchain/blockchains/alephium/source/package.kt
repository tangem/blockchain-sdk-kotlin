package com.tangem.blockchain.blockchains.alephium.source

internal val minimalGas: GasBox = GasBox.unsafe(20000)
internal val dustUtxoAmount: U256 = ALPH.nanoAlph(1000000)