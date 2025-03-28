package com.tangem.blockchain.common.smartcontract

import org.komputing.khex.extensions.toHexString

/**
 * Interface for implementing smart contracts.
 * @property prefix     describes smart contract method signature
 * @property data       compiled data ready to send in transaction. Usually build by concatenating method signature
 * and passed method arguments
 * @property dataHex    hex representation of data, used in internal logic
 */
interface SmartContractMethod {
    val prefix: String
    val data: ByteArray
    val dataHex: String
        get() = data.toHexString()
}