package com.tangem.blockchain.blockchains.polkadot

import com.tangem.blockchain.blockchains.polkadot.network.accounthealthcheck.AccountResponse
import com.tangem.blockchain.blockchains.polkadot.network.accounthealthcheck.ExtrinsicDetailResponse
import com.tangem.blockchain.blockchains.polkadot.network.accounthealthcheck.ExtrinsicListResponse

/**
 * Methods to check account and transactions
 */
interface AccountCheckProvider {

    /** Get list of extrinsic with offset [afterExtrinsicId] */
    suspend fun getExtrinsicList(afterExtrinsicId: Long? = null): ExtrinsicListResponse

    /** Get extrinsic detail by [hash] */
    suspend fun getExtrinsicDetail(hash: String): ExtrinsicDetailResponse

    /** Get nonce and extrinsic info of account */
    suspend fun getAccountInfo(): AccountResponse
}