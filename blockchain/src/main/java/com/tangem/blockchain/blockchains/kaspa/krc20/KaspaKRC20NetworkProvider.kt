package com.tangem.blockchain.blockchains.kaspa.krc20

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

interface KaspaKRC20NetworkProvider : NetworkProvider {
    suspend fun getBalances(address: String, tokens: List<Token>): Result<List<KaspaKRC20InfoResponse>>
}

data class KaspaKRC20InfoResponse(
    val token: Token,
    val balance: BigDecimal,
)