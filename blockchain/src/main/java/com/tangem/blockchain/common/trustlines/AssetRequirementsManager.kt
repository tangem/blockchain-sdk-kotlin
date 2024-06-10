package com.tangem.blockchain.common.trustlines

import com.tangem.blockchain.common.CryptoCurrencyType
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.SimpleResult

/**
 * Responsible for the token association creation (Hedera) and trust line setup (XRP, Stellar, Aptos, Algorand and other).
 */
interface AssetRequirementsManager {

    suspend fun hasRequirements(currencyType: CryptoCurrencyType): Boolean

    suspend fun requirementsCondition(currencyType: CryptoCurrencyType): AssetRequirementsCondition?

    suspend fun fulfillRequirements(currencyType: CryptoCurrencyType, signer: TransactionSigner): SimpleResult
}