package com.tangem.blockchain.blockchains.hedera

import com.hedera.hashgraph.sdk.TokenId
import com.tangem.Log
import com.tangem.blockchain.common.BlockchainSdkError

internal object HederaUtils {

    fun createTokenId(contractAddress: String): TokenId {
        return try {
            TokenId.fromString(contractAddress)
        } catch (e: Exception) {
            TokenId.fromSolidityAddress(contractAddress)
        } catch (e: Exception) {
            Log.error { e.message.orEmpty() }
            throw BlockchainSdkError.CustomError(e.message.orEmpty())
        }
    }
}