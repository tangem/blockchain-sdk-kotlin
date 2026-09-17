package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.core.createAssociatedSolanaTokenAddress
import com.tangem.blockchain.blockchains.solana.solanaj.model.SolanaTokenAccountInfo
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import org.p2p.solanaj.core.PublicKey

/**
 * Picks one token account per mint: the associated one whenever it exists, because send always spends from it and
 * the wallet must not show the balance of one account while spending from another.
 */
internal object SolanaTokenAccountSelector {

    fun uniqueAccountsByMint(
        accounts: List<SolanaTokenAccountInfo>,
        ownerAccountId: String,
    ): Map<String, SolanaTokenAccountInfo> {
        return accounts
            .groupBy(SolanaTokenAccountInfo::mint)
            .mapNotNull { (mint, accountsOfMint) ->
                val preferredAddress = preferredAccountAddress(
                    addresses = accountsOfMint.map(SolanaTokenAccountInfo::address),
                    mint = mint,
                    ownerAccountId = ownerAccountId,
                )

                accountsOfMint.firstOrNull { it.address == preferredAddress }?.let { mint to it }
            }
            .toMap()
    }

    fun preferredAccountAddress(addresses: List<String>, mint: String, ownerAccountId: String): String? {
        // Deriving the associated address is a program-address search - skip it when there is nothing to choose.
        if (addresses.size < 2) return addresses.firstOrNull()

        val associatedAddresses = SolanaTokenProgram.ID.entries.mapNotNullTo(destination = hashSetOf()) { programId ->
            associatedTokenAddress(ownerAccountId = ownerAccountId, mintAddress = mint, tokenProgramId = programId)
        }

        return addresses.firstOrNull { it in associatedAddresses }
            // No associated account yet - keep the first one so the token is still surfaced.
            ?: addresses.firstOrNull()
    }

    private fun associatedTokenAddress(
        ownerAccountId: String,
        mintAddress: String,
        tokenProgramId: SolanaTokenProgram.ID,
    ): String? = runCatching {
        createAssociatedSolanaTokenAddress(
            account = PublicKey(ownerAccountId),
            mint = PublicKey(mintAddress),
            tokenProgramId = tokenProgramId,
        ).getOrNull()?.toBase58()
    }.getOrNull()
}