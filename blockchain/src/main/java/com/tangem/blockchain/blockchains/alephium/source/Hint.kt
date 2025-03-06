package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.blockchains.alephium.source.serde.Serde

@JvmInline
internal value class Hint constructor(val value: Int) {

    val isAssetType: Boolean get() = value and 1 == 1

    companion object {

        // We don't use Serde[Int] here as the value of Hint is random, no need of serde optimization
        val serde: Serde<Hint> = Serde
            .bytesSerde(4)
            .xmap({ bs -> Hint(Bytes.toIntUnsafe(bs)) }, { hint -> Bytes.from(hint.value) })

        fun from(assetOutput: AssetOutput): Hint = ofAsset(assetOutput.lockupScript.scriptHint)

        fun ofAsset(scriptHint: ScriptHint): Hint = Hint(scriptHint.value)
    }
}