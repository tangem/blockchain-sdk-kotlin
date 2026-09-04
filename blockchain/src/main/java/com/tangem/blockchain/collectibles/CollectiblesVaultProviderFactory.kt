package com.tangem.blockchain.collectibles

import com.tangem.blockchain.blockchains.solana.SolanaNetworkService
import com.tangem.blockchain.collectibles.providers.solana.SolanaCollectiblesVaultProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.network.MultiNetworkProvider
import org.p2p.solanaj.core.PublicKey

/** Picks a [CollectiblesVaultProvider] by blockchain, mirroring the other feature-provider factories. */
internal object CollectiblesVaultProviderFactory {

    /**
     * [networkProvider] is erased at this boundary so the factory can be called from any assembly, and cast to the
     * one the chosen provider needs — the same trick `YieldSupplyProviderFactory` uses. Safe because the cast happens
     * inside a branch that already pinned the blockchain.
     */
    @Suppress("UNCHECKED_CAST")
    fun makeProvider(
        wallet: Wallet,
        networkProvider: MultiNetworkProvider<out NetworkProvider>,
    ): CollectiblesVaultProvider = when (wallet.blockchain) {
        Blockchain.SolanaTestnet -> SolanaCollectiblesVaultProvider(
            coldOwner = PublicKey(wallet.address),
            multiNetworkProvider = networkProvider as MultiNetworkProvider<SolanaNetworkService>,
        )
        else -> DefaultCollectiblesVaultProvider
    }
}