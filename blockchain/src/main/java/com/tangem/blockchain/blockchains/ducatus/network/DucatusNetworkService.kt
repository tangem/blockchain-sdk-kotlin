package com.tangem.blockchain.blockchains.ducatus.network

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.ducatus.network.bitcore.BitcoreNetworkProvider
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.API_DUCATUS

class DucatusNetworkService : BitcoreNetworkProvider(API_DUCATUS) {

    override suspend fun getFee(): Result<BitcoinFee> {
        // Bitcore is used only in Ducatus and fee is hardcoded there
        return Result.Failure(BlockchainSdkError.CustomError("Fee should be hardcoded in ducatus"))
    }

    override suspend fun getSignatureCount(address: String): Result<Int> {
        // Bitcore is used only in Ducatus and we don't check signature count
        return Result.Failure(BlockchainSdkError.CustomError("Signature count is not appliable for ducatus"))
    }
}
