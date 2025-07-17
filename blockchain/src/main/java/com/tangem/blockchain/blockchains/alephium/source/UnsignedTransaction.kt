package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.blockchains.alephium.source.serde.*
import com.tangem.blockchain.blockchains.alephium.source.serde.Serde.Companion.Flags
import com.tangem.blockchain.common.BlockchainSdkError
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.append
import kotlinx.io.bytestring.buildByteString

/** Up to one new token might be issued in each transaction exception for the coinbase transaction
 * The id of the new token will be hash of the first input
 *
 * @param version
 *   the version of the tx
 * @param networkId
 *   the id of the chain which can accept the tx
 * @param scriptOpt
 *   optional script for invoking stateful contracts
 * @param gasAmount
 *   the amount of gas can be used for tx execution
 * @param inputs
 *   a vector of TxInput
 * @param fixedOutputs
 *   a vector of TxOutput. ContractOutput are put in front of AssetOutput
 */

internal data class UnsignedTransaction(
    val version: Byte,
    val networkId: NetworkId,
    val gasAmount: GasBox,
    val gasPrice: GasPrice,
    val inputs: List<TxInput>,
    val fixedOutputs: List<AssetOutput>,
) {

    val id: TransactionId
        get() = TransactionId.hash(serde.serialize(this))

    @Suppress("LargeClass")
    companion object {

        val serde = object : Serde<UnsignedTransaction> {
            override fun serialize(input: UnsignedTransaction): ByteString = buildByteString {
                append(byteSerde.serialize(input.version))
                append(NetworkId.serde.serialize(input.networkId))
                // null for scriptOpt: Option[StatefulScript]
                append(byteSerde.serialize(Flags.noneB))
                append(GasBox.serde.serialize(input.gasAmount))
                append(u256Serde.serialize(input.gasPrice.value))
                append(
                    listSerializer(Serializer<TxInput> { item -> TxInput.serde.serialize(item) })
                        .serialize(input.inputs),
                )
                append(
                    listSerializer(Serializer<AssetOutput> { item -> AssetOutput.serde.serialize(item) })
                        .serialize(input.fixedOutputs),
                )
            }

            override fun _deserialize(input: ByteString): Result<Staging<UnsignedTransaction>> {
                val pair0 = byteSerde._deserialize(input).getOrElse { return Result.failure(it) }
                val pair1 = NetworkId.serde._deserialize(pair0.rest).getOrElse { return Result.failure(it) }
                // for scriptOpt: Option[StatefulScript] expect Flags.noneB as null
                val pair2 = byteSerde._deserialize(pair1.rest).getOrElse { return Result.failure(it) }
                val pair3 = GasBox.serde._deserialize(pair2.rest).getOrElse { return Result.failure(it) }
                val pair4 = u256Serde._deserialize(pair3.rest).getOrElse { return Result.failure(it) }

                val pair5 = listDeserializer { item -> TxInput.serde._deserialize(item) }
                    ._deserialize(pair4.rest).getOrElse { return Result.failure(it) }
                val pair6 = listDeserializer { item -> AssetOutput.serde._deserialize(item) }
                    ._deserialize(pair5.rest).getOrElse { return Result.failure(it) }

                val value = UnsignedTransaction(
                    version = pair0.value,
                    networkId = pair1.value,
                    gasAmount = pair3.value,
                    gasPrice = GasPrice(pair4.value),
                    inputs = pair5.value,
                    fixedOutputs = pair6.value,
                )
                return Result.success(Staging(value, pair6.rest))
            }
        }

        fun buildTxOutputs(
            fromLockupScript: LockupScript.Asset,
            inputs: List<Pair<AssetOutputRef, AssetOutput>>,
            outputInfos: TxOutputInfo,
            gas: GasBox,
            gasPrice: GasPrice,
        ): Result<Pair<List<AssetOutput>, List<AssetOutput>>> {
            val gasFee = preCheckBuildTx(inputs, gas, gasPrice)
                .onFailure { return Result.failure(it) }.getOrThrow()
            checkMinimalAlphPerOutput(outputInfos)
            checkTokenValuesNonZero(outputInfos)
            val txOutputs = buildOutputs(outputInfos)
            val changeOutputs = calculateChangeOutputs(fromLockupScript, inputs, txOutputs, gasFee)
                .onFailure { return Result.failure(it) }.getOrThrow()
            return Result.success(txOutputs to changeOutputs)
        }

        private fun calculateChangeOutputs(
            fromLockupScript: LockupScript.Asset,
            inputs: List<Pair<AssetOutputRef, AssetOutput>>,
            txOutputs: List<AssetOutput>,
            gasFee: U256,
        ): Result<List<AssetOutput>> {
            val inputUTXOView = inputs.map { it.second }
            val alphRemainder = calculateAlphRemainder(
                inputUTXOView.map { it.amount },
                txOutputs.map { it.amount },
                gasFee,
            )
                .onFailure { return Result.failure(it) }.getOrThrow()
            val tokensRemainder = calculateTokensRemainder(
                inputUTXOView.flatMap { it.tokens },
                txOutputs.flatMap { it.tokens },
            )
                .onFailure { return Result.failure(it) }.getOrThrow()
            val changeOutputs = calculateChangeOutputs(alphRemainder, tokensRemainder, fromLockupScript)
            return changeOutputs
        }

        private fun calculateChangeOutputs(
            alphRemainder: U256,
            tokensRemainder: List<Pair<TokenId, U256>>,
            fromLockupScript: LockupScript.Asset,
        ): Result<List<AssetOutput>> {
            return if (alphRemainder == U256.Zero && tokensRemainder.isEmpty()) {
                Result.success(emptyList())
            } else {
                val tokenDustAmount = dustUtxoAmount.mulUnsafe(U256.unsafe(tokensRemainder.size))
                val totalDustAmount = tokenDustAmount.addUnsafe(dustUtxoAmount)

                when {
                    alphRemainder == tokenDustAmount || alphRemainder >= totalDustAmount -> Result.success(
                        buildOutputs(
                            TxOutputInfo(
                                lockupScript = fromLockupScript,
                                attoAlphAmount = alphRemainder,
                                tokens = tokensRemainder,
                                lockTime = null,
                                additionalDataOpt = null,
                            ),
                        ),
                    )

                    tokensRemainder.isEmpty() -> Result.failure(
                        Exception(
                            "Not enough ALPH for ALPH change output, expected $dustUtxoAmount, got $alphRemainder",
                        ),
                    )

                    alphRemainder < tokenDustAmount -> Result.failure(
                        Exception(
                            "Not enough ALPH for token change output, expected $tokenDustAmount, got $alphRemainder",
                        ),
                    )

                    else -> Result.failure(
                        Exception(
                            "Not enough ALPH for ALPH and token change output, " +
                                "expected $totalDustAmount, got $alphRemainder",
                        ),
                    )
                }
            }
        }

        private fun buildOutputs(outputInfo: TxOutputInfo): List<AssetOutput> {
            val toLockupScript = outputInfo.lockupScript
            val attoAlphAmount = outputInfo.attoAlphAmount
            val tokens = outputInfo.tokens
            val lockTimeOpt = outputInfo.lockTime
            val additionalDataOpt = outputInfo.additionalDataOpt
            val tokenOutputs = tokens.map { token ->
                AssetOutput(
                    dustUtxoAmount,
                    toLockupScript,
                    lockTimeOpt ?: TimeStamp.zero,
                    listOf(token),
                    additionalDataOpt ?: ByteString(),
                )
            }
            val alphRemaining = attoAlphAmount
                .sub(dustUtxoAmount.mulUnsafe(U256.unsafe(tokens.size))) ?: U256.Zero
            return if (alphRemaining == U256.Zero) {
                tokenOutputs
            } else {
                val alphOutput = AssetOutput(
                    maxOf(alphRemaining, dustUtxoAmount),
                    toLockupScript,
                    lockTimeOpt ?: TimeStamp.zero,
                    listOf(),
                    additionalDataOpt ?: ByteString(),
                )
                tokenOutputs + alphOutput
            }
        }

        private fun checkMinimalAlphPerOutput(output: TxOutputInfo): Result<Unit> {
            return check(
                failCondition = output.attoAlphAmount < dustUtxoAmount,
                "Tx output value is too small, avoid spreading dust",
            )
        }

        private fun checkTokenValuesNonZero(output: TxOutputInfo): Result<Unit> {
            return check(
                failCondition = output.tokens.any { it.second.isZero },
                "Value is Zero for one or many tokens in the transaction output",
            )
        }

        private fun check(failCondition: Boolean, errorMessage: String): Result<Unit> {
            return if (!failCondition) {
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException(errorMessage))
            }
        }

        private fun preCheckBuildTx(
            inputs: List<Pair<AssetOutputRef, AssetOutput>>,
            gas: GasBox,
            gasPrice: GasPrice,
        ): Result<U256> {
            check(gas < minimalGas, "gas < minimalGas")
            check(gasPrice.value > ALPH.MaxALPHValue, "gasPrice.value > ALPH.MaxALPHValue")
            checkWithMaxTxInputNum(inputs)
            checkUniqueInputs(inputs)
            return Result.success(gasPrice * gas)
        }

        private fun checkWithMaxTxInputNum(assets: List<Pair<AssetOutputRef, AssetOutput>>): Result<Unit> {
            return check(
                failCondition = assets.size > ALPH.MaxTxInputNum,
                "Too many inputs for the transfer, consider to reduce the amount to send, " +
                    "or use the `sweep-address` endpoint to consolidate the inputs first",
            )
        }

        private fun checkUniqueInputs(assets: List<Pair<AssetOutputRef, AssetOutput>>): Result<Unit> {
            return check(
                failCondition = assets.size > assets.map { it.first }.toSet().size,
                "Inputs not unique",
            )
        }

        private fun calculateAlphRemainder(inputs: List<U256>, outputs: List<U256>, gasFee: U256): Result<U256> {
            val inputSum = inputs.fold(U256.Zero) { acc, sum ->
                acc.add(sum) ?: return Result.failure(RuntimeException("Input amount overflow"))
            }
            val outputAmount = outputs.fold(U256.Zero) { acc, sum ->
                acc.add(sum) ?: return Result.failure(RuntimeException("Output amount overflow"))
            }
            val remainder0 = inputSum.sub(outputAmount) ?: return Result.failure(RuntimeException("Not enough balance"))
            val remainder =
                remainder0.sub(gasFee) ?: run {
                    val sumDec = outputAmount.v.toBigDecimal() + gasFee.v.toBigDecimal()
                    val amountDec = outputAmount.v.toBigDecimal()
                    val error = BlockchainSdkError.Alephium.NotEnoughBalance(
                        gotSum = sumDec,
                        expectedAmount = amountDec,
                        customMessage = "Not enough balance: got ${sumDec.toPlainString()}, " +
                            "expected ${amountDec.toPlainString()}",
                    )
                    return Result.failure(error)
                }
            return Result.success(remainder)
        }

        private fun calculateTokensRemainder(
            inputsIn: List<Pair<TokenId, U256>>,
            outputsIn: List<Pair<TokenId, U256>>,
        ): Result<List<Pair<TokenId, U256>>> {
            val inputs = calculateTotalAmountPerToken(inputsIn).onFailure { return Result.failure(it) }.getOrThrow()
            val outputs = calculateTotalAmountPerToken(outputsIn).onFailure { return Result.failure(it) }.getOrThrow()
            checkNoNewTokensInOutputs(inputs, outputs)
            val remainder = calculateRemainingTokens(inputs, outputs)
                .onFailure { return Result.failure(it) }.getOrThrow()
            return Result.success(remainder.filterNot { it.second == U256.Zero })
        }

        private fun calculateRemainingTokens(
            inputTokens: List<Pair<TokenId, U256>>,
            outputTokens: List<Pair<TokenId, U256>>,
        ): Result<List<Pair<TokenId, U256>>> {
            return Result.success(
                inputTokens.fold(listOf()) { acc, (inputId, inputAmount) ->
                    val outputAmount = outputTokens.find { it.first == inputId }?.second ?: U256.Zero
                    val remainder: U256 = inputAmount.sub(outputAmount)
                        ?: return Result.failure(RuntimeException("Not enough balance for token $inputId"))
                    acc.plus(inputId to remainder)
                },
            )
        }

        private fun calculateTotalAmountPerToken(tokens: List<Pair<TokenId, U256>>): Result<List<Pair<TokenId, U256>>> {
            return Result.success(
                tokens.fold(listOf()) { acc, (id, amount) ->
                    val index = acc.indexOfFirst { it.first == id }
                    if (index == -1) {
                        acc + Pair(id, amount)
                    } else {
                        val amt: U256 = acc[index].second.add(amount)
                            ?: return Result.failure(RuntimeException("Amount overflow for token $id"))
                        val list = acc.toMutableList()
                        list[index] = Pair(id, amt)
                        list
                    }
                },
            )
        }

        private fun checkNoNewTokensInOutputs(
            inputs: List<Pair<TokenId, U256>>,
            outputs: List<Pair<TokenId, U256>>,
        ): Result<Unit> {
            val newTokens = outputs.map { it.first }.toSet() - inputs.map { it.first }.toSet()
            return check(
                failCondition = newTokens.isNotEmpty(),
                errorMessage = "New tokens found in outputs: $newTokens",
            )
        }

        @Suppress("LongParameterList")
        fun buildTransferTx(
            fromLockupScript: LockupScript.Asset,
            fromUnlockScript: UnlockScript,
            inputs: List<Pair<AssetOutputRef, AssetOutput>>,
            outputInfos: TxOutputInfo,
            gas: GasBox,
            gasPrice: GasPrice,
            networkId: NetworkId,
        ): Result<UnsignedTransaction> {
            return buildTxOutputs(
                fromLockupScript,
                inputs,
                outputInfos,
                gas,
                gasPrice,
            ).map { (txOutputs, changeOutputs) ->
                UnsignedTransaction(
                    version = 0,
                    networkId = networkId,
                    gasAmount = gas,
                    gasPrice = gasPrice,
                    inputs = buildInputs(fromUnlockScript, inputs),
                    fixedOutputs = txOutputs + changeOutputs,
                )
            }
        }

        private fun buildInputs(
            fromUnlockScript: UnlockScript,
            inputs: List<Pair<AssetOutputRef, AssetOutput>>,
        ): List<TxInput> {
            return inputs.mapIndexed { index, (outputRef, _) ->
                if (index == 0) {
                    TxInput(outputRef, fromUnlockScript)
                } else {
                    TxInput(outputRef, UnlockScript.SameAsPrevious)
                }
            }
        }

        // Note: this would calculate excess dustAmount to cover the complicated cases
        fun calculateTotalAmountNeeded(outputInfo: TxOutputInfo): Triple<U256, List<Pair<TokenId, U256>>, Int> {
            var totalAlphAmount = U256.Zero
            var totalTokens = emptyMap<TokenId, U256>()
            var totalOutputLength = 0
            val tokenDustAmount = dustUtxoAmount.mulUnsafe(U256.unsafe(outputInfo.tokens.size))
            val outputLength = outputInfo.tokens.size + // UTXOs for token
                if (outputInfo.attoAlphAmount <= tokenDustAmount) 0 else 1 // UTXO for ALPH
            val alphAmount = maxOf(outputInfo.attoAlphAmount, dustUtxoAmount.mulUnsafe(U256.unsafe(outputLength)))
            val newAlphAmount = totalAlphAmount.add(alphAmount) ?: U256.Zero
            val newTotalTokens = updateTokens(totalTokens, outputInfo.tokens).getOrElse { mapOf<TokenId, U256>() }
            totalAlphAmount = newAlphAmount
            totalTokens = newTotalTokens
            totalOutputLength += outputLength

            val outputLengthSender = totalTokens.size /*+ 1*/ // magic plus 1 incorrect total amount, comment for now
            val alphAmountSender = dustUtxoAmount.mulUnsafe(U256.unsafe(outputLengthSender))
            val finalAlphAmount = totalAlphAmount.add(alphAmountSender) ?: U256.Zero
            return Triple(
                finalAlphAmount,
                totalTokens.toList(),
                totalOutputLength + outputLengthSender,
            )
        }

        private fun updateTokens(
            totalTokens: Map<TokenId, U256>,
            newTokens: List<Pair<TokenId, U256>>,
        ): Result<Map<TokenId, U256>> {
            val result = newTokens.fold(totalTokens) { acc, (tokenId, amount) ->
                val totalAmount = acc[tokenId]
                if (totalAmount == null) {
                    acc + (tokenId to amount)
                } else {
                    val newAmount = totalAmount.add(amount)
                        ?: return Result.failure(RuntimeException("Amount overflow for token $tokenId"))
                    acc + (tokenId to newAmount)
                }
            }
            return Result.success(result)
        }
    }
}

internal data class TxOutputInfo(
    val lockupScript: LockupScript.Asset,
    val attoAlphAmount: U256,
    val tokens: List<Pair<TokenId, U256>>,
    val lockTime: TimeStamp?,
    val additionalDataOpt: ByteString? = null,
)