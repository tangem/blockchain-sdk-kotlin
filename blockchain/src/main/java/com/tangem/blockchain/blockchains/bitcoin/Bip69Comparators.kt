package com.tangem.blockchain.blockchains.bitcoin

import org.bitcoinj.core.TransactionInput
import org.bitcoinj.core.TransactionOutput

/**
 * Bip69comparators implementation from [horizontalsystems.bitcoincore.utils.Bip69]
 */
object Bip69Comparators {

    private const val before = -1
    private const val equal = 0
    private const val after = 1

    val outputComparator = kotlin.Comparator<TransactionOutput> { output1, output2 ->
        //sort by amount first
        val valueCompareResult = output1.value.compareTo(output2.value)
        if (valueCompareResult != equal) {
            return@Comparator valueCompareResult
        }

        val keyHash1 = output1?.outPointFor?.hash ?: return@Comparator after
        val keyHash2 = output2?.outPointFor?.hash ?: return@Comparator before

        //when amounts are equal, sort by hash
        val hashCompareResult = compareByteArrays(keyHash1.bytes, keyHash2.bytes)
        if (hashCompareResult != equal) {
            return@Comparator hashCompareResult
        }

        //sort by hash size
        return@Comparator keyHash1.bytes.size.compareTo(keyHash2.bytes.size)
    }

    val inputComparator = kotlin.Comparator<TransactionInput> { input1, input2 ->
        //sort by hash first
        val result = compareByteArrays(input1.outpoint.hash.bytes, input2.outpoint.hash.bytes)
        if (result != equal) {
            return@Comparator result
        }

        //sort by index
        return@Comparator input1.index.compareTo(input2.index)
    }

    private fun compareByteArrays(b1: ByteArray, b2: ByteArray): Int {
        var pos = 0

        while (pos < b1.size && pos < b2.size) {
            val result = (b1[pos].toInt() and 0xff).compareTo(b2[pos].toInt() and 0xff)
            if (result == equal) {
                pos++
            } else {
                return result
            }
        }

        return equal
    }
}