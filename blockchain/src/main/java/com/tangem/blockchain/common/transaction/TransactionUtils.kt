package com.tangem.blockchain.common.transaction

import java.math.BigDecimal

internal inline fun <T> getMinimumRequiredUTXOsToSend(
    unspentOutputs: List<T>,
    transactionAmount: BigDecimal,
    transactionFeeAmount: BigDecimal,
    crossinline unspentToAmount: (T) -> BigDecimal,
): List<T> {
    require(transactionAmount >= 0.0.toBigDecimal())
    require(transactionFeeAmount >= 0.0.toBigDecimal())

    val amount = transactionAmount + transactionFeeAmount

    // insufficient balance
    if (unspentOutputs.sumOf { unspentToAmount(it) } < amount) {
        return unspentOutputs.sortedByDescending { unspentToAmount(it) }
    }

    val sortedUnspent = unspentOutputs.sortedBy { unspentToAmount(it) }
    val unusedSortedUnspent = sortedUnspent.toMutableList()

    val outputsRes = mutableListOf<T>()
    var currentTotal = BigDecimal.ZERO

    while (currentTotal < amount && unusedSortedUnspent.size > 0) {
        val binRes = unusedSortedUnspent.binarySearchBy(amount - currentTotal) { unspentToAmount(it) }

        val utxoIndex = when {
            binRes < 0 -> {
                val possibleIndex = -binRes - 1
                if (possibleIndex == unusedSortedUnspent.size) {
                    possibleIndex - 1
                } else {
                    possibleIndex
                }
            }
            else -> binRes
        }

        val utxo = unusedSortedUnspent[utxoIndex]
        currentTotal += unspentToAmount(utxo)
        outputsRes.add(utxo)
        unusedSortedUnspent.remove(utxo)
    }

    return outputsRes
}

internal inline fun <T> getMinimumRequiredUTXOsToSendSatoshi(
    unspentOutputs: List<T>,
    transactionSatoshiAmount: Long,
    transactionSatoshiFeeAmount: Long,
    crossinline unspentToSatoshiAmount: (T) -> Long,
): List<T> {
    require(transactionSatoshiAmount >= 0)
    require(transactionSatoshiFeeAmount >= 0)

    val amount = transactionSatoshiAmount + transactionSatoshiFeeAmount

    // insufficient balance
    if (unspentOutputs.sumOf { unspentToSatoshiAmount(it) } < amount) {
        return unspentOutputs.sortedByDescending { unspentToSatoshiAmount(it) }
    }

    val sortedUnspent = unspentOutputs.sortedBy { unspentToSatoshiAmount(it) }
    val unusedSortedUnspent = sortedUnspent.toMutableList()

    val outputsRes = mutableListOf<T>()
    var currentTotal = 0L

    while (currentTotal < amount && unusedSortedUnspent.size > 0) {
        val binRes = unusedSortedUnspent.binarySearchBy(amount - currentTotal) { unspentToSatoshiAmount(it) }

        val utxoIndex = when {
            binRes < 0 -> {
                val possibleIndex = -binRes - 1
                if (possibleIndex == unusedSortedUnspent.size) {
                    possibleIndex - 1
                } else {
                    possibleIndex
                }
            }
            else -> binRes
        }

        val utxo = unusedSortedUnspent[utxoIndex]
        currentTotal += unspentToSatoshiAmount(utxo)
        outputsRes.add(utxo)
        unusedSortedUnspent.remove(utxo)
    }

    return outputsRes
}