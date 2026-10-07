package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Blockchain
import java.math.BigInteger

private val WEI_PER_TINYXCN = BigInteger.TEN.pow(10)

/**
 * Native coin [wei] value in the form the chain accepts in a transaction.
 *
 * Onyx keeps XCN in 8 decimals behind an 18-decimals JSON-RPC relay that rejects any value which is not a whole number
 * of tinyxcn (10^10 wei), so the sub-tinyxcn remainder is dropped.
 */
internal fun Blockchain.toAcceptedCoinValue(wei: BigInteger): BigInteger = when (this) {
    Blockchain.Onyx, Blockchain.OnyxTestnet -> wei - wei.mod(WEI_PER_TINYXCN)
    else -> wei
}