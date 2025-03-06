package com.tangem.blockchain.blockchains.alephium.source

internal sealed interface OutputInfo {
    val ref: TxOutputRef
    val output: TxOutput
}

internal data class AssetOutputInfo(
    override val ref: AssetOutputRef,
    override val output: AssetOutput,
    val outputType: OutputType,
) : OutputInfo

internal sealed interface OutputType {
    val cachedLevel: Int
}

internal data object PersistedOutput : OutputType {
    override val cachedLevel: Int = 0
}

internal object UnpersistedBlockOutput : OutputType {
    override val cachedLevel: Int = 1
}

internal object MemPoolOutput : OutputType {
    override val cachedLevel: Int = 2
}