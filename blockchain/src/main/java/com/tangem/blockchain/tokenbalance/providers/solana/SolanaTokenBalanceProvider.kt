package com.tangem.blockchain.tokenbalance.providers.solana

import com.tangem.blockchain.blockchains.solana.SolanaNetworkService
import com.tangem.blockchain.blockchains.solana.solanaj.model.SolanaTokenAccountInfo
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.tokenbalance.TokenBalanceProvider
import com.tangem.blockchain.tokenbalance.models.TokenBalance
import org.p2p.solanaj.core.PublicKey
import java.math.BigDecimal

internal class SolanaTokenBalanceProvider(
    private val multiNetworkProvider: MultiNetworkProvider<SolanaNetworkService>,
) : TokenBalanceProvider {

    override suspend fun getTokenBalances(walletAddress: String): List<TokenBalance> {
        val account = PublicKey(walletAddress)
        val mainAccountInfo = when (
            val result = multiNetworkProvider.performRequest {
                getMainAccountInfo(account, cardTokens = null)
            }
        ) {
            is Result.Success -> result.data
            is Result.Failure -> return emptyList()
        }

        if (mainAccountInfo.value == null) return emptyList()

        return mainAccountInfo.tokensByMint.values
            .filter { it.solAmount > BigDecimal.ZERO }
            .map { it.toTokenBalance() }
    }

    private fun SolanaTokenAccountInfo.toTokenBalance(): TokenBalance = TokenBalance(
        contractAddress = mint,
        amount = solAmount,
        isNativeToken = false,
    )
}