package com.tangem.blockchain.common.smartcontract

import org.komputing.khex.extensions.toHexString

/**
 * Interface for implementing smart contract call data.
 * @property methodId   unique identifier to differentiate between different functions or methods within a smart contract
 * @property data       compiled data ready to send in transaction. Usually build by concatenating method signature
 * and passed method arguments
 * @property dataHex    hex representation of data, used in internal logic
 */
interface SmartContractCallData {
    val methodId: String
    val data: ByteArray
    val dataHex: String
        get() = data.toHexString()

    fun validate(): Boolean
}