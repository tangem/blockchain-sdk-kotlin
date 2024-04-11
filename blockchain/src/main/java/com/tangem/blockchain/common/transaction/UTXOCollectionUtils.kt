package com.tangem.blockchain.common.transaction

import java.math.BigDecimal

/**
 * Method for collecting minimum required UTXOs for transaction (based on binary search)
 * The collection is processed as follows:
 *  1. Sorts the list of all available UTXOs that can be used in a transaction
 *  2. By iterating through this list, the algorithm selects the closest UTXO to the required transaction amount
 *     (amount + commission)
 *  3. After the previous iteration, the total required amount will be reduced by the previously selected UTXO
 *     and it ready to search for the next UTXO closest to this amount
 *  4. When all the UTXOs necessary to complete the requested transaction amount have been collected,
 *     the function returns a list of UTXOs sorted in descending order of the amount
 *
 *  @throws IllegalArgumentException if transactionAmount < 0 or transactionFeeAmount < 0
 *  @param unspentOutputs the list of UTXOs that will be used for selection
 *  @param transactionAmount requested transaction amount for UTXO selection
 *  @param transactionFeeAmount requested transaction fee amount for UTXO selection
 *  @param unspentToAmount UTXO object to it's amount mapping
 *
 *  @return a list of selected UTXOs sorted by descending amount.
 *  If the sum of all UTXOs is less than the requested transaction amount,
 *  the function returns the list passed to the function, sorted in descending order of the amount
 *
 *  @see getMinimumRequiredUTXOsToSendSatoshi
 */
internal inline fun <T> getMinimumRequiredUTXOsToSend(
    unspentOutputs: List<T>,
    transactionAmount: BigDecimal,
    transactionFeeAmount: BigDecimal,
    crossinline unspentToAmount: (T) -> BigDecimal,
): List<T> {
    require(transactionAmount >= BigDecimal.ZERO)
    require(transactionFeeAmount >= BigDecimal.ZERO)

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

/**
 * ### Use with caution
 * ### Only for blockchains with maximum possible total value in satoshi <= [Long.MAX_VALUE]
 *
 * Method for collecting minimum required UTXOs for transaction (based on binary search)
 * The faster version of [getMinimumRequiredUTXOsToSend]
 *
 * The collection is processed as follows:
 *  1. Sorts the list of all available UTXOs that can be used in a transaction
 *  2. By iterating through this list, the algorithm selects the closest UTXO to the required transaction amount
 *     (amount + commission)
 *  3. After the previous iteration, the total required amount will be reduced by the previously selected UTXO
 *     and it ready to search for the next UTXO closest to this amount
 *  4. When all the UTXOs necessary to complete the requested transaction amount have been collected,
 *     the function returns a list of UTXOs sorted in descending order of the amount
 *
 *  @throws IllegalArgumentException if transactionAmount < 0 or transactionFeeAmount < 0
 *  @param unspentOutputs the list of UTXOs that will be used for selection
 *  @param transactionSatoshiAmount requested transaction amount for UTXO selection
 *  @param transactionSatoshiFeeAmount requested transaction fee amount for UTXO selection
 *  @param unspentToSatoshiAmount UTXO object to it's amount mapping
 *
 *  @return a list of selected UTXOs sorted by descending amount.
 *  If the sum of all UTXOs is less than the requested transaction amount,
 *  the function returns the list passed to the function, sorted in descending order of the amount
 *
 *  @see getMinimumRequiredUTXOsToSend
 */
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