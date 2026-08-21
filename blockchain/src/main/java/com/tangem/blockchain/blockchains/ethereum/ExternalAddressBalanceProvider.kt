package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.network.ExternalAddressBalances
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result

/**
 * Reads the balances of an EVM address that this wallet does not own.
 *
 * Implemented by the EVM wallet managers, reached the way the other narrow capabilities are — by casting the
 * manager, as with `ReserveAmountProvider` or `UtxoAmountLimitProvider`:
 *
 * ```
 * val balances = (manager as? ExternalAddressBalanceProvider)?.getExternalAddressBalances(address, tokens)
 * ```
 *
 * The caller keeps its own wallet manager for the network and asks it about a foreign address, so the request goes
 * through the same provider rotation and failover as everything else.
 */
interface ExternalAddressBalanceProvider {

    /**
     * Coin balance and the balances of [tokens] on [address].
     *
     * Reads nothing but balances: no transaction counts and no history, since a caller asking about a foreign
     * address has no use for them. The wallet's own yield-supply module is not consulted either — it belongs to
     * this wallet, not to [address].
     */
    suspend fun getExternalAddressBalances(address: String, tokens: Set<Token>): Result<ExternalAddressBalances>
}