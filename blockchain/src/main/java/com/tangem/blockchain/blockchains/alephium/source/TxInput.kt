package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.blockchains.alephium.source.serde.Serde
import com.tangem.blockchain.blockchains.alephium.source.serde.Tuple2
import com.tangem.blockchain.blockchains.alephium.source.serde.forProduct2

internal data class TxInput(val outputRef: AssetOutputRef, val unlockScript: UnlockScript) {

    companion object {
        // Note that the serialization has to put outputRef in the first 32 bytes for the sake of trie indexing
        val serde = forProduct2(
            pack = ::TxInput,
            unpack = { Tuple2(it.outputRef, it.unlockScript) },
            serdeA0 = AssetOutputRef.serde,
            serdeA1 = UnlockScript.serde,
        )
    }
}

internal sealed interface TxOutputRef {
    val hint: Hint
    val key: Key

    val isAssetType: Boolean
    val isContractType: Boolean

    data class Key(val value: Blake2b256) {
        companion object {
            val keySerde: Serde<Key> = Blake2b256.serde.xmap(::Key) { it.value }
        }
    }

    companion object {

        // fun key(txId: TransactionId, outputIndex: Int): Key {
        //     return Key(Blake2b.hash(txId.bytes().concat(Bytes.from(outputIndex))))
        // }
    }
}

internal data class AssetOutputRef constructor(override val hint: Hint, override val key: TxOutputRef.Key) :
    TxOutputRef {
    override val isAssetType: Boolean = true
    override val isContractType: Boolean = false

    override fun hashCode(): Int = key.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is AssetOutputRef -> hint == other.hint && key == other.key
        else -> false
    }

    companion object {
        val serde = forProduct2(
            pack = ::AssetOutputRef,
            unpack = { Tuple2(it.hint, it.key) },
            serdeA0 = Hint.serde,
            serdeA1 = TxOutputRef.Key.keySerde,
        ).validate { outputRef ->
            if (outputRef.hint.isAssetType) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Expect AssetOutputRef, got ContractOutputRef"))
            }
        }
    }
}

internal data class ContractOutputRef constructor(override val hint: Hint, override val key: TxOutputRef.Key) :
    TxOutputRef {
    override val isAssetType: Boolean = false
    override val isContractType: Boolean = true
}