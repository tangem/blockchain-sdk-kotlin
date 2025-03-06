package com.tangem.blockchain.blockchains.alephium.source

import kotlin.math.pow

@JvmInline
internal value class GasEstimationMultiplier private constructor(val value: Double) {
    operator fun times(gas: GasBox): GasBox {
        val numerator = (value * Denominator).toInt()
        return GasBox.unsafe(gas.value * numerator / Denominator)
    }

    companion object {
        private const val MaxPrecision = 2
        private val Denominator = 10.0.pow(MaxPrecision).toInt()

        fun from(multiplier: Double?): Result<GasEstimationMultiplier?> {
            return if (multiplier == null) {
                Result.success(null)
            } else {
                from(multiplier).map { it }
            }
        }

        fun from(multiplier: Double): Result<GasEstimationMultiplier> {
            return when {
                multiplier < 1.0 || multiplier > 2.0 -> {
                    Result.failure(
                        IllegalArgumentException(
                            "Invalid gas estimation multiplier, expected a value between [1.0, 2.0]",
                        ),
                    )
                }

                multiplier.toString().let { str ->
                    val precision = if (str.contains(".")) str.length - str.indexOf(".") - 1 else 0
                    precision > MaxPrecision
                } -> {
                    Result.failure(
                        IllegalArgumentException(
                            "Invalid gas estimation multiplier precision, maximum allowed precision is $MaxPrecision",
                        ),
                    )
                }

                else -> {
                    Result.success(GasEstimationMultiplier(multiplier))
                }
            }
        }
    }
}