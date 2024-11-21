package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.blockchains.alephium.source.serde.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.append
import kotlinx.io.bytestring.buildByteString

internal sealed interface TxOutput {
    val amount: U256
    val lockupScript: LockupScript
    val tokens: List<Pair<TokenId, U256>>

    val hint: Hint

    val isAsset: Boolean

    val isContract: Boolean
        get() = !isAsset

    fun payGasUnsafe(fee: U256): TxOutput

    companion object {
        fun from(amount: U256, tokens: List<Pair<TokenId, U256>>, lockupScript: LockupScript.Asset): List<TxOutput>? =
            from(amount, tokens, lockupScript, TimeStamp.zero)

        fun from(
            amount: U256,
            tokens: List<Pair<TokenId, U256>>,
            lockupScript: LockupScript.Asset,
            lockTime: TimeStamp,
        ): List<TxOutput>? {
            val outputs = Array<TxOutput?>(tokens.size + 1) { null }
            tokens.forEachIndexed { index, token ->
                outputs[index] =
                    AssetOutput(dustUtxoAmount, lockupScript, lockTime, listOf(token), ByteString())
            }
            val totalTokenDustAmount = dustUtxoAmount.mulUnsafe(U256.unsafe(tokens.size))
            return when {
                amount == totalTokenDustAmount -> {
                    outputs.filterNotNull()
                }

                amount >= totalTokenDustAmount.addUnsafe(dustUtxoAmount) -> {
                    val alphRemaining = amount.subUnsafe(totalTokenDustAmount)
                    outputs[tokens.size] =
                        AssetOutput(
                            alphRemaining,
                            lockupScript,
                            lockTime,
                            listOf(),
                            ByteString(),
                        )
                    outputs.mapNotNull { it }
                }

                else -> null
            }
        }
    }
}

/** @param amount
 * the number of ALPH in the output
 * @param lockupScript
 * guarding script for unspent output
 * @param lockTime
 * the timestamp until when the tx can be used. it's zero by default, and will be replaced with
 * block timestamp in worldstate if it's zero we could implement relative time lock based on
 * block timestamp
 * @param tokens
 * secondary tokens in the output
 * @param additionalData
 * data payload for additional information
 */
internal data class AssetOutput(
    override val amount: U256,
    override val lockupScript: LockupScript.Asset,
    val lockTime: TimeStamp,
    override val tokens: List<Pair<TokenId, U256>>,
    val additionalData: ByteString,
) : TxOutput {
    override val isAsset: Boolean
        get() = true

    override val hint: Hint
        get() = Hint.from(this)

    override fun payGasUnsafe(fee: U256): AssetOutput =
        AssetOutput(amount.subUnsafe(fee), lockupScript, lockTime, tokens, additionalData)

    companion object {
        private val tokenSerde = tuple2(TokenId.serde, u256Serde)
        val serde = object : Serde<AssetOutput> {
            override fun serialize(input: AssetOutput): ByteString = buildByteString {
                append(u256Serde.serialize(input.amount))
                append(LockupScript.serde.serialize(input.lockupScript))
                append(TimeStamp.serde.serialize(input.lockTime))
                append(
                    listSerializer(Serializer<Pair<TokenId, U256>> { item -> tokenSerde.serialize(item) })
                        .serialize(input.tokens),
                )
                append(byteArraySerde.serialize(input.additionalData))
            }

            override fun _deserialize(input: ByteString): Result<Staging<AssetOutput>> {
                val pair0 = u256Serde._deserialize(input).getOrElse { return Result.failure(it) }
                val pair1 = LockupScript.serde._deserialize(pair0.rest).getOrElse { return Result.failure(it) }
                val pair2 = TimeStamp.serde._deserialize(pair1.rest).getOrElse { return Result.failure(it) }
                val pair3 = listDeserializer { item -> tokenSerde._deserialize(item) }
                    ._deserialize(pair2.rest).getOrElse { return Result.failure(it) }
                val pair4 = byteArraySerde._deserialize(pair3.rest).getOrElse { return Result.failure(it) }
                val value = AssetOutput(
                    amount = pair0.value,
                    lockupScript = pair1.value,
                    lockTime = pair2.value,
                    tokens = pair3.value,
                    additionalData = pair4.value,
                )
                return Result.success(Staging(value, pair4.rest))
            }
        }
    }
}