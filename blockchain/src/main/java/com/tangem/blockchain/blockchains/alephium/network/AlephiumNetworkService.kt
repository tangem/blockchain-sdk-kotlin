package com.tangem.blockchain.blockchains.alephium.network

import com.tangem.blockchain.blockchains.alephium.models.AlephiumFee
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.MultiNetworkProvider
import java.math.BigDecimal

internal class AlephiumNetworkService(
    providers: List<AlephiumNetworkProvider>,
    blockchain: Blockchain,
) : AlephiumNetworkProvider {

    override val baseUrl: String get() = multiJsonRpcProvider.currentProvider.baseUrl

    private val multiJsonRpcProvider = MultiNetworkProvider(providers, blockchain)

    override suspend fun getInfo(address: String): Result<AlephiumResponse.Utxos> =
        multiJsonRpcProvider.performRequest(AlephiumNetworkProvider::getInfo, address)

    override suspend fun getFee(amount: BigDecimal, destination: String, publicKey: String): Result<AlephiumFee> =
        multiJsonRpcProvider.performRequest { getFee(amount, destination, publicKey) }

    override suspend fun submitTx(unsignedTx: String, signature: String): Result<AlephiumResponse.SubmitTx> {
        return multiJsonRpcProvider.performRequest { submitTx(unsignedTx, signature) }
    }
}