package com.tangem.blockchain.blockchains.solana.nft

import com.tangem.blockchain.blockchains.solana.SolanaNetworkService
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import io.ktor.util.decodeBase64Bytes
import org.p2p.solanaj.core.PublicKey

internal class SolanaNFTAccount(val owner: String, val data: ByteArray)

internal interface SolanaNFTNetworkService {

    /** Success with null when the account does not exist; a wallet (system account) comes with empty data. */
    suspend fun readAccount(address: PublicKey): Result<SolanaNFTAccount?>

    suspend fun rentExemption(space: Long): Result<Long>
}

internal class MultiNetworkSolanaNFTNetworkService(
    private val multiNetworkProvider: MultiNetworkProvider<SolanaNetworkService>,
) : SolanaNFTNetworkService {

    override suspend fun readAccount(address: PublicKey): Result<SolanaNFTAccount?> =
        when (val result = multiNetworkProvider.performRequest { getAccountInfoIfExist(address) }) {
            is Result.Success -> Result.Success(
                SolanaNFTAccount(
                    owner = result.data.owner,
                    data = result.data.data.firstOrNull()?.decodeBase64Bytes() ?: ByteArray(0),
                ),
            )
            is Result.Failure -> {
                if (result.error is BlockchainSdkError.AccountNotFound) {
                    Result.Success(null)
                } else {
                    result
                }
            }
        }

    override suspend fun rentExemption(space: Long): Result<Long> =
        multiNetworkProvider.performRequest { minimalBalanceForRentExemption(space) }
}