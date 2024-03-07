package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.core.createAssociatedSolanaTokenAddress
import com.tangem.blockchain.blockchains.solana.solanaj.model.SolanaSplAccountInfo
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import org.p2p.solanaj.core.PublicKey

internal class SolanaTokenAccountInfoFinder(
    private val multiNetworkProvider: MultiNetworkProvider<SolanaNetworkService>,
) {

    suspend fun getTokenAccountInfoAndTokenProgramId(
        account: PublicKey,
        mint: PublicKey,
    ): Result<Pair<SolanaSplAccountInfo, SolanaTokenProgram.ID>> {
        val resultForTokenProgram = getTokenAccountInfoIfExist(
            account = account,
            mint = mint,
            programId = SolanaTokenProgram.ID.TOKEN,
        )

        return when (resultForTokenProgram) {
            is Result.Failure -> {
                getTokenAccountInfoIfExist(
                    account = account,
                    mint = mint,
                    programId = SolanaTokenProgram.ID.TOKEN_2022,
                ).map { it to SolanaTokenProgram.ID.TOKEN_2022 }
            }
            is Result.Success -> {
                resultForTokenProgram.map { it to SolanaTokenProgram.ID.TOKEN }
            }
        }
    }

    suspend fun getTokenAccountInfoIfExist(
        account: PublicKey,
        mint: PublicKey,
        programId: SolanaTokenProgram.ID,
    ): Result<SolanaSplAccountInfo> {
        val associatedTokenAddress = createAssociatedSolanaTokenAddress(
            account = account,
            mint = mint,
            tokenProgramId = programId,
        ).getOrElse {
            return Result.Failure(BlockchainSdkError.Solana.FailedToCreateAssociatedAccount)
        }

        val tokenAccountInfo = multiNetworkProvider.performRequest {
            getTokenAccountInfoIfExist(associatedTokenAddress)
        }.successOr { return it }

        return Result.Success(tokenAccountInfo)
    }
}