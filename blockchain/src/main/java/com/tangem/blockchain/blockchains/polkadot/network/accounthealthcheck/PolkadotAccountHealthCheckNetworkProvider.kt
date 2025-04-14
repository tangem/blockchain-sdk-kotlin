package com.tangem.blockchain.blockchains.polkadot.network.accounthealthcheck

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result

interface PolkadotAccountHealthCheckNetworkProvider : NetworkProvider {

    suspend fun getExtrinsicsList(address: String, afterExtrinsicId: Long?): Result<ExtrinsicListResponse>

    suspend fun getExtrinsicDetail(hash: String): Result<ExtrinsicDetailResponse>

    suspend fun getAccountInfo(key: String): Result<AccountResponse>
}