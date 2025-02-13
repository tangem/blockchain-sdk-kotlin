package com.tangem.blockchain.blockchains.alephium.network

import com.tangem.blockchain.blockchains.alephium.models.AlephiumFee
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

internal interface AlephiumNetworkProvider : NetworkProvider {

    suspend fun getInfo(address: String): Result<AlephiumResponse.Utxos>

    suspend fun getFee(amount: BigDecimal, destination: String, publicKey: String): Result<AlephiumFee>

    suspend fun submitTx(unsignedTx: String, signature: String): Result<AlephiumResponse.SubmitTx>
}