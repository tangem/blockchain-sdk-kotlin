package com.tangem.blockchain.blockchains.polkadot.network

import com.tangem.blockchain.extensions.Result
import io.emeraldpay.polkaj.tx.ExtrinsicContext
import io.emeraldpay.polkaj.types.Address
import java.math.BigDecimal

interface PolkadotNetworkProvider {
    val host: String

    suspend fun getBalance(address: String): Result<BigDecimal>

    suspend fun getFee(builtTransaction: ByteArray): Result<BigDecimal>

    suspend fun sendTransaction(builtTransaction: ByteArray): Result<String>

    suspend fun extrinsicContext(address: String): Result<ExtrinsicContext>
}