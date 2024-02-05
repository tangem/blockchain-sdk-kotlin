package com.tangem.blockchain.blockchains.aptos.network

import com.tangem.blockchain.blockchains.aptos.models.AptosAccountInfo
import com.tangem.blockchain.blockchains.aptos.models.AptosTransactionInfo
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result

/**
 * Aptos network provider
 *
 * @author Andrew Khokhlov on 11/01/2024
 */
internal interface AptosNetworkProvider : NetworkProvider {

    /** Get account information by [address] */
    suspend fun getAccountInfo(address: String): Result<AptosAccountInfo>

    /**
     * Get normal gas price.
     * Prioritizing transactions isn't supports due to difficult scheme of fee calculating.
     */
    suspend fun getGasUnitPrice(): Result<Long>

    /**
     * Calculate gas price unit that required to send transaction
     *
     * @param transaction unsigned transaction
     */
    suspend fun calculateUsedGasPriceUnit(transaction: AptosTransactionInfo): Result<Long>

    /** Submit signed transaction using [jsonOutput] */
    suspend fun submitTransaction(jsonOutput: String): Result<String>
}
