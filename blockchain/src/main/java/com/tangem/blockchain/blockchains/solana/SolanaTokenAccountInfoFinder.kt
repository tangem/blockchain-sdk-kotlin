package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.blockchains.solana.solanaj.core.createAssociatedSolanaTokenAddress
import com.tangem.blockchain.blockchains.solana.solanaj.model.SolanaSplAccountInfo
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgramId
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
    ): Result<Pair<SolanaSplAccountInfo, SolanaTokenProgramId>> {
        val resultForTokenProgram = getTokenAccountInfoIfExist(
            account = account,
            mint = mint,
            programId = SolanaTokenProgramId.TOKEN,
        )

        return when (resultForTokenProgram) {
            is Result.Failure -> {
                getTokenAccountInfoIfExist(
                    account = account,
                    mint = mint,
                    programId = SolanaTokenProgramId.TOKEN_2022,
                ).map { it to SolanaTokenProgramId.TOKEN_2022 }
            }
            is Result.Success -> {
                resultForTokenProgram.map { it to SolanaTokenProgramId.TOKEN }
            }
        }
    }

    suspend fun getTokenAccountInfoIfExist(
        account: PublicKey,
        mint: PublicKey,
        programId: SolanaTokenProgramId,
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
