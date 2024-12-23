package com.tangem.blockchain.common.trustlines

import com.tangem.blockchain.common.CryptoCurrencyType
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.SimpleResult

interface AssetRequirementsManager {

    suspend fun requirementsCondition(currencyType: CryptoCurrencyType): AssetRequirementsCondition?

    suspend fun fulfillRequirements(currencyType: CryptoCurrencyType, signer: TransactionSigner): SimpleResult

    suspend fun discardRequirements(currencyType: CryptoCurrencyType): SimpleResult
}