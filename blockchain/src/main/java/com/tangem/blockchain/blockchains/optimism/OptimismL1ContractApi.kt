@file:Suppress("MagicNumber")

package com.tangem.blockchain.blockchains.optimism

import org.kethereum.contract.abi.types.encodeTypes
import org.kethereum.contract.abi.types.model.types.DynamicSizedBytesETHType
import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.kethereum.model.createEmptyTransaction

/**
 * Generated from contract ABI by https://github.com/komputing/kethabi
 */

internal val FourByteDecimals: ByteArray = byteArrayOf(49, 60, -27, 103)
internal val FourByteGasPrice: ByteArray = byteArrayOf(-2, 23, 59, -105)
internal val FourByteGetL1Fee: ByteArray = byteArrayOf(73, -108, -114, 14)

internal class OptimismGasL1TransactionGenerator(address: Address) {

    private val tx: Transaction = createEmptyTransaction().apply { to = address }

    /**
     * Signature: decimals()
     * 4Byte: 313ce567
     */
    fun decimals(): Transaction = decimalsETHTyped()

    /**
     * Signature: gasPrice()
     * 4Byte: fe173b97
     */
    fun gasPrice(): Transaction = gasPriceETHTyped()

    /**
     * Signature: getL1Fee(bytes)
     * 4Byte: 49948e0e
     */
    fun getL1Fee(data: ByteArray): Transaction {
        return getL1FeeETHTyped(DynamicSizedBytesETHType.ofNativeKotlinType(data))
    }

    private fun decimalsETHTyped() = tx.copy(input = FourByteDecimals + encodeTypes())

    private fun gasPriceETHTyped() = tx.copy(input = FourByteGasPrice + encodeTypes())

    private fun getL1FeeETHTyped(data: DynamicSizedBytesETHType) = tx.copy(
        input = FourByteGetL1Fee + encodeTypes(data),
    )
}
