package com.tangem.blockchain.blockchains.alephium.source

internal data class ScriptHint(val value: Int) {

    companion object {
        fun fromHash(hash: Blake2b): ScriptHint {
            return fromHash(DjbHash.intHash(hash.bytes()))
        }

        private fun fromHash(hash: Int): ScriptHint {
            return ScriptHint(hash or 1)
        }
    }
}